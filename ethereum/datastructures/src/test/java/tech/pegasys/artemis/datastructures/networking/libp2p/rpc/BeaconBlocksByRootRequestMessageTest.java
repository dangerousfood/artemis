/*
 * Copyright 2019 ConsenSys AG.
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

package tech.pegasys.artemis.datastructures.networking.libp2p.rpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.datastructures.util.SimpleOffsetSerializer;

class BeaconBlocksByRootRequestMessageTest {
  @Test
  public void shouldRoundTripViaSsz() {
    final BeaconBlocksByRootRequestMessage request =
        new BeaconBlocksByRootRequestMessage(
            List.of(
                Bytes32.ZERO,
                Bytes32.fromHexStringLenient("0x01"),
                Bytes32.fromHexStringLenient("0x02")));
    final Bytes data = SimpleOffsetSerializer.serialize(request);
    final BeaconBlocksByRootRequestMessage result =
        SimpleOffsetSerializer.deserialize(data, BeaconBlocksByRootRequestMessage.class);

    assertThat(result).isEqualToComparingFieldByField(request);
  }
}
