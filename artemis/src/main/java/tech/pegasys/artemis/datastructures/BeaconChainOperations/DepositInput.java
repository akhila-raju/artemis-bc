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

package tech.pegasys.artemis.datastructures.BeaconChainOperations;

import tech.pegasys.artemis.ethereum.core.Hash;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt384;

public class DepositInput {

  private UInt384 pubkey;
  private Hash withdrawal_credentials;
  private Hash randao_commitment;
  private Bytes48[] proof_of_possession;

  public DepositInput(UInt384 pubkey, Hash withdrawal_credentials, Hash randao_commitment,
                      Bytes48[] proof_of_possession) {
    this.pubkey = pubkey;
    this.withdrawal_credentials = withdrawal_credentials;
    this.randao_commitment = randao_commitment;
    this.proof_of_possession = proof_of_possession;
  }

}
