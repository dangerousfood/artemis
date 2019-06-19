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

package tech.pegasys.artemis.datastructures.util;

import com.google.common.primitives.UnsignedLong;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.Hash;
import tech.pegasys.artemis.datastructures.state.BeaconState;
import tech.pegasys.artemis.datastructures.state.Validator;

import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.toIntExact;
import static tech.pegasys.artemis.datastructures.Constants.SHARD_COUNT;
import static tech.pegasys.artemis.datastructures.Constants.SHUFFLE_ROUND_COUNT;
import static tech.pegasys.artemis.datastructures.Constants.SLOTS_PER_EPOCH;
import static tech.pegasys.artemis.datastructures.Constants.TARGET_COMMITTEE_SIZE;
import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.bytes_to_int;
import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.generate_seed;
import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.get_current_epoch;
import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.get_epoch_committee_count;
import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.int_to_bytes;
import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.max;
import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.min;
import static tech.pegasys.artemis.datastructures.util.ValidatorsUtil.get_active_validator_indices;

public class CrosslinkCommitteeUtil {

  public static Integer get_shuffled_index(int index, int index_count, Bytes32 seed){
    //Return the shuffled validator index corresponding to ``seed`` (and ``index_count``).
    checkArgument(index < index_count);
    checkArgument(index_count <= Math.pow(2, 40));

    //Swap or not (https://link.springer.com/content/pdf/10.1007%2F978-3-642-32009-5_1.pdf)
    //See the 'generalized domain' algorithm on page 3
    for(int round = 0; round< SHUFFLE_ROUND_COUNT; round++){
      long pivot = bytes_to_int(Bytes.wrap(ArrayUtils.subarray(Hash.sha2_256(Bytes.concatenate(seed, int_to_bytes(round, 1))).toArray(), 0, 8)));
      long flip = (pivot + index_count - index) % index_count;
      long position = max(UnsignedLong.valueOf(index), UnsignedLong.valueOf(flip)).longValue();
      Bytes32 source = Hash.sha2_256(Bytes.concatenate(seed, int_to_bytes(round, 1), int_to_bytes(Math.floorDiv(position, 256l), 4)));
      byte byteValue = source.get(toIntExact(Math.floorDiv((position % 256), 8l)));
      int bit = (byteValue >> (position % 8)) % 2;
      index = (bit == 1) ? (int)flip : index;
    }
    return index;
  }
  public static List<Integer> compute_committee(List<Integer> indices, Bytes32 seed, int index, int count){
    int start = Math.floorDiv(indices.size() * index, count);
    int end = Math.floorDiv(indices.size() * (index + 1), count);

    for(int i = start; i < end; i++)indices.set(i, get_shuffled_index(i, indices.size(), seed));
    return indices;
  }
  public static List<Integer> get_crosslink_committee(BeaconState state, UnsignedLong epoch, UnsignedLong shard){
    return compute_committee(
            get_active_validator_indices(state, epoch),
            generate_seed(state, epoch),
            (shard.intValue() + SHARD_COUNT - get_epoch_start_shard(state, epoch).intValue()) % SHARD_COUNT,
            get_epoch_committee_count(state, epoch).intValue()
            );
  }
  public static UnsignedLong get_epoch_start_shard(BeaconState state, UnsignedLong epoch){
    checkArgument(epoch.compareTo(get_current_epoch(state).plus(UnsignedLong.ONE)) <= 0);
    UnsignedLong check_epoch = get_current_epoch(state).plus(UnsignedLong.ONE);
    UnsignedLong shard = state.getLatest_start_shard().plus(get_shard_delta(state, get_current_epoch(state))).mod(SHARD_COUNT);

    while(check_epoch.compareTo(epoch) > 0){
      check_epoch = check_epoch.minus(UnsignedLong.ONE);
      shard = (shard.plus(UnsignedLong.valueOf(SHARD_COUNT)).minus(get_shard_delta(state, check_epoch))).mod(SHARD_COUNT);
    }
    return shard;
  }

  public static UnsignedLong get_shard_delta(BeaconState state, UnsignedLong epoch){
    //Return the number of shards to increment ``state.latest_start_shard`` during ``epoch``.
    return min(get_epoch_committee_count(state, epoch), UnsignedLong.valueOf(SHARD_COUNT - Math.floorDiv(SHARD_COUNT , SLOTS_PER_EPOCH)));
  }
}
