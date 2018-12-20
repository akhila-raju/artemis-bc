package tech.pegasys.artemis.ethereum.core;

import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytes32;
import tech.pegasys.artemis.util.uint.UInt256Bytes;

import java.util.ArrayList;
import java.util.Collections;

import static tech.pegasys.artemis.util.bytes.Bytes32.fromHexStringStrict;
import static tech.pegasys.artemis.util.bytes.Bytes32.intToBytes32;

public class TreeHash {

  private static int SSZ_CHUNK_SIZE = 128;

  public Object[] hash_tree_root(ArrayList<BytesValue> value) {
    ArrayList<BytesValue> lst = new ArrayList<BytesValue>(value.size());
    public Bytes32 hash_tree_root(Bytes32 value) {
      // if the output is less than 32 bytes, right-zero-pad it to 32 bytes
      if (Bytes32.lessThan32Bytes(value)) {
        return Bytes32.rightPad(value);
      }
      Bytes32[] lst = new Bytes32;
      for (int i = 0; i < value.size(); i++) {
        lst.add(hash_tree_root(new ArrayList<BytesValue>(BytesValue.wrap(value.get(i).extractArray())));
        lst[i] = hash_tree_root(Bytes32.wrap(new byte[]{value.get(i)}));
      }
      return merkle_hash(lst);
      // return merkle_hash([hash_tree_root(item) for item in value]);

      // if the output is less than 32 bytes, right-zero-pad it to 32 bytes
    }


    /**
     * Merkle tree hash of a list of homogenous, non-empty items.
     * @param lst
     * @return
     */
    private Hash merkle_hash(ArrayList<Bytes32> lst) {
      // Store length of list (to compensate for non-bijectiveness of padding)
      Bytes32 datalen = intToBytes32(lst.size());
      ArrayList<Bytes32> chunkz = new ArrayList<Bytes32>(lst.size());

      if (lst.size() == 0) {
        // Handle empty list case
        chunkz = new ArrayList<Bytes32>(Collections.nCopies(SSZ_CHUNK_SIZE, Bytes32.FALSE));

      } else if (lst.get(0).size() < SSZ_CHUNK_SIZE) {
        // See how many items fit in a chunk
        int items_per_chunk = SSZ_CHUNK_SIZE / lst.get(0).size();

        // Build a list of chunks based on the number of items in the chunk
        chunkz = new ArrayList<Bytes32>(items_per_chunk);
        for (int i = 0; i < lst.size(); i += items_per_chunk) {

          String new_val = new String;
          for (int j = 0; j < items_per_chunk; j++) {
            new_val += lst.get(i + j).extractArray().toString();
          }

          chunkz.set(i, fromHexStringStrict(new_val));
        }

      } else {
        // Leave large items alone
        chunkz = lst;
      }

      // Tree-hash
      while (chunkz.size() > 1) {
        if (chunkz.size() % 2 == 1) {
          chunkz.addAll(Collections.nCopies(SSZ_CHUNK_SIZE, Bytes32.FALSE));
        }
        chunkz = new ArrayList<Bytes32>();
        for (int i = 0; i < chunkz.size(); i += 2) {
          MutableBytes32 chunk_to_hash = MutableBytes32.create();
          UInt256Bytes.add(chunkz.get(i), chunkz.get(i+1), chunk_to_hash);
          chunkz.add(Hash.hash(chunk_to_hash));
        }
      }

      // Return hash of root and length data
      MutableBytes32 sum_ = MutableBytes32.create();
      UInt256Bytes.add(chunkz.get(0), datalen, sum_);
      return Hash.hash(sum_);
    }


  }

}