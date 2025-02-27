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

package com.aliyun.emr.rss.service.deploy.master.clustermeta;

import com.aliyun.emr.rss.common.meta.WorkerInfo;

public class MetaUtil {
  private MetaUtil() {
  }

  public static WorkerInfo addrToInfo(ResourceProtos.WorkerAddress address) {
    return new WorkerInfo(address.getHost(),
      address.getRpcPort(), address.getPushPort(), address.getFetchPort());
  }

  public static ResourceProtos.WorkerAddress infoToAddr(WorkerInfo info) {
    return ResourceProtos.WorkerAddress.newBuilder()
      .setHost(info.host())
      .setRpcPort(info.rpcPort())
      .setPushPort(info.pushPort())
      .setFetchPort(info.fetchPort())
      .build();
  }
}
