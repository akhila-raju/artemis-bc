package tech.pegasys.artemis.util.bls;

import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.uint.UInt384;
import tech.pegasys.artemis.util.uint.UInt64;

public class BLSVerify {

  // TODO: Need to update UInt384 to UInt384[].
  public static boolean bls_verify(UInt384 pubkey, Bytes32 message, UInt384 signature, UInt64 domain) {
    return true;
  }

}

