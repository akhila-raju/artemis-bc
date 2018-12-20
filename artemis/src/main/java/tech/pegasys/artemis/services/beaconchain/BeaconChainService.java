/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.artemis.services.beaconchain;
import tech.pegasys.artemis.Constants;
import tech.pegasys.artemis.datastructures.BeaconChainBlocks.BeaconBlock;
import tech.pegasys.artemis.datastructures.BeaconChainOperations.Deposit;
import tech.pegasys.artemis.datastructures.BeaconChainState.CandidatePoWReceiptRootRecord;
import tech.pegasys.artemis.datastructures.BeaconChainState.CrosslinkRecord;
import tech.pegasys.artemis.datastructures.BeaconChainState.ForkData;
import tech.pegasys.artemis.datastructures.BeaconChainState.PendingAttestationRecord;
import tech.pegasys.artemis.datastructures.BeaconChainState.ShardCommittee;
import tech.pegasys.artemis.datastructures.BeaconChainState.ShardReassignmentRecord;
import tech.pegasys.artemis.datastructures.BeaconChainState.ValidatorRecord;
import tech.pegasys.artemis.ethereum.core.Hash;
import tech.pegasys.artemis.factories.EventBusFactory;
import tech.pegasys.artemis.services.ServiceInterface;
import tech.pegasys.artemis.state.BeaconState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.uint.UInt64;

import static java.lang.Math.toIntExact;
import static tech.pegasys.artemis.Constants.ACTIVE;
import static tech.pegasys.artemis.Constants.GWEI_PER_ETH;
import static tech.pegasys.artemis.Constants.INITIAL_FORK_VERSION;
import static tech.pegasys.artemis.Constants.INITIAL_SLOT_NUMBER;
import static tech.pegasys.artemis.Constants.MAX_DEPOSIT;
import static tech.pegasys.artemis.Constants.SHARD_COUNT;
import static tech.pegasys.artemis.Constants.ZERO_HASH;

public class BeaconChainService implements ServiceInterface{

    private final EventBus eventBus;
    private BeaconState state;
    private StateTransition stateTransition;

    public BeaconChainService(){
        this.eventBus = EventBusFactory.getInstance();
        this.state = new BeaconState();
        this.stateTransition = new StateTransition();
    }


    private BeaconState get_initial_beacon_state(Deposit[] initial_validator_deposits,
                                                 int genesis_time, Hash processed_pow_receipt_root) {

        Hash[] latest_block_roots = new Hash[LATEST_BLOCK_ROOTS_LENGTH];
        Arrays.fill(latest_block_roots, ZERO_HASH);

        CrosslinkRecord[] latest_crosslinks = new CrosslinkRecord[SHARD_COUNT];
        for (int i = 0; i < SHARD_COUNT; i++) {
            latest_crosslinks[i] = new CrosslinkRecord(UInt64.valueOf(INITIAL_SLOT_NUMBER), Hash.ZERO);
        }

        BeaconState state = BeaconState(
            // Misc
            UInt64.valueOf(INITIAL_SLOT_NUMBER),
            genesis_time,
            new ForkData(UInt64.valueOf(INITIAL_FORK_VERSION), UInt64.valueOf(INITIAL_FORK_VERSION),
                UInt64.valueOf(INITIAL_SLOT_NUMBER)),

            // Validator registry
            new ArrayList<ValidatorRecord>(),
            UInt64.valueOf(INITIAL_SLOT_NUMBER),
            UInt64.valueOf(0),
            ZERO_HASH,

            // Randomness and committees
            ZERO_HASH,
            ZERO_HASH,
            new ShardCommittee[][],
            new int[][],
            new ShardReassignmentRecord[],

            // Finality
            UInt64.valueOf(INITIAL_SLOT_NUMBER),
            UInt64.valueOf(INITIAL_SLOT_NUMBER,
                UInt64.valueOf(0),
                UInt64.valueOf(INITIAL_SLOT_NUMBER),

                // Recent state
                latest_crosslinks,
                latest_block_roots,
                new UInt64[],
                new PendingAttestationRecord[],
                new Hash[],

                // PoW receipt root
                processed_pow_receipt_root,
                new CandidatePoWReceiptRootRecord[]);

        // handle initial deposits and activations
        for (int i = 0; i < initial_validator_deposits.length; i++) {
            Deposit deposit = initial_validator_deposits[i];
            int validator_index = process_deposit(state, deposit.deposit_data.deposit_input.pubkey,
                deposit.deposit_data.value, deposit.deposit_data.deposit_input.proof_of_possession,
                deposit.deposit_data.deposit_input.withdrawal_credentials,
                deposit.deposit_data.deposit_input.randao_commitment);

            if (toIntExact(state.validator_registry.get(validator_index).balance.getValue())
                >= (MAX_DEPOSIT * GWEI_PER_ETH)) {
                update_validator_status(state, validator_index, ACTIVE);
            }
        }

        // set initial committee shuffling
        ShardCommittee[][] initial_shuffling = get_new_shuffling(ZERO_HASH, initial_validator_registry, 0);
        state.shard_committees_at_slots = initial_shuffling + initial_shuffling;

        // set initial persistent shuffling
        int[] active_validator_indices = get_active_validator_indices(state.validator_registry);
        state.persistent_committees = split(shuffle(active_validator_indices, ZERO_HASH), SHARD_COUNT);

        return state;
    }


    @Override
    public void init(){
        this.eventBus.register(this);
    }

    @Override
    public void start(){
        slotScheduler();
    }

    @Override
    public void stop(){
    }

    @Subscribe
    public void onChainStarted(ChainStartEvent event){
        System.out.println("ChainStart Event Detected");
    }

    @Subscribe
    public void onValidatorRegistered(ValidatorRegistrationEvent event){
        System.out.println("Validator Registration Event detected");
        //System.out.println("   Validator Number: " + validatorRegisteredEvent.getInfo());
    }

    @Subscribe
    public void onNewSlot(Date date){
        System.out.println("****** New Slot at: " + date + " ******");

        stateTransition.initiate(this.state, new BeaconBlock());

    }

    @Subscribe
    public void onNewBlock(BeaconBlock beaconBlock){
        System.out.println("New Beacon Block Event detected");
        System.out.println("   Block Number:" + beaconBlock.getSlot());
    }

   // slot scheduler fires an event that tells us when it is time for a new slot
    protected void slotScheduler(){
        int intialDelay = 0;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                EventBus eventBus = EventBusFactory.getInstance();
                eventBus.post(new Date());
            }
        }, intialDelay, Constants.SLOT_DURATION , TimeUnit.SECONDS);
    }

}
