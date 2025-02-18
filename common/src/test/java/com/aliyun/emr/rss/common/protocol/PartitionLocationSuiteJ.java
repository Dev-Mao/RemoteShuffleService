/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aliyun.emr.rss.common.protocol;

import org.junit.Test;

public class PartitionLocationSuiteJ {

  private final int reduceId = 0;
  private final int epoch = 0;
  private final String host = "localhost";
  private final int rpcPort = 3;
  private final int pushPort = 1;
  private final int fetchPort = 2;
  private final PartitionLocation.Mode mode = PartitionLocation.Mode.Master;
  private final PartitionLocation peer = new PartitionLocation(
          reduceId, epoch, host, rpcPort, pushPort, fetchPort, PartitionLocation.Mode.Slave);

  @Test
  public void testGetCorrectMode() {
    byte masterMode = 0;
    byte slaveMode = 1;

    assert PartitionLocation.getMode(masterMode) == PartitionLocation.Mode.Master;
    assert PartitionLocation.getMode(slaveMode) == PartitionLocation.Mode.Slave;

    for (int i = 2; i < 255; ++i) {
      byte otherMode = (byte) i;
      // Should we return slave mode when the parameter passed in is neither 0 or 1?
      assert PartitionLocation.getMode(otherMode) == PartitionLocation.Mode.Slave;
    }
  }

  @Test
  public void testReduceIdNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    PartitionLocation location2 = new PartitionLocation(
        reduceId + 1, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testEpochNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    PartitionLocation location2 = new PartitionLocation(
        reduceId, epoch + 1, host, rpcPort, pushPort, fetchPort, mode, peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testHostNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    PartitionLocation location2 = new PartitionLocation(
        reduceId, epoch, "remoteHost", rpcPort, pushPort, fetchPort, mode, peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testPushPortNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    PartitionLocation location2 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort + 1, fetchPort, mode, peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testFetchPortNotEqualMakePartitionLocationDifferent() {
    PartitionLocation location1 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    PartitionLocation location2 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort + 1, mode, peer);
    checkEqual(location1, location2, false);
  }

  @Test
  public void testModeNotEqualNeverMakePartitionLocationDifferent() {
    PartitionLocation location1 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    PartitionLocation location2 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, PartitionLocation.Mode.Slave, peer);
    PartitionLocation location3 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    checkEqual(location1, location2, true);
    checkEqual(location1, location3, true);
    checkEqual(location2, location3, true);
  }

  @Test
  public void testPeerNotEqualNeverMakePartitionLocationDifferent() {
    PartitionLocation location1 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    PartitionLocation location2 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, location1);
    PartitionLocation location3 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    checkEqual(location1, location2, true);
    checkEqual(location1, location3, true);
    checkEqual(location2, location3, true);
  }

  @Test
  public void testAllFieldEqualMakePartitionLocationEqual() {
    PartitionLocation location1 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    PartitionLocation location2 = new PartitionLocation(
        reduceId, epoch, host, rpcPort, pushPort, fetchPort, mode, peer);
    checkEqual(location1, location2, true);
  }

  private void checkEqual(
      PartitionLocation location1, PartitionLocation location2, boolean shouldEqual) {
    String errorMessage = "Need location1 " + location1 + " and location2 " + location2 + " are " +
      (shouldEqual ? "" : "not ") + "equal, but " + (shouldEqual ? "not " : "") + "equal.";
    assert location1.equals(location2) == shouldEqual: errorMessage;
  }
}
