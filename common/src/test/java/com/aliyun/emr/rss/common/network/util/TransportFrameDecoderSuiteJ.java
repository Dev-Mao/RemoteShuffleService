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

package com.aliyun.emr.rss.common.network.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TransportFrameDecoderSuiteJ {

  private static Random RND = new Random();

  @AfterClass
  public static void cleanup() {
    RND = null;
  }

  @Test
  public void testFrameDecoding() throws Exception {
    TransportFrameDecoder decoder = new TransportFrameDecoder();
    ChannelHandlerContext ctx = mockChannelHandlerContext();
    ByteBuf data = createAndFeedFrames(100, decoder, ctx);
    verifyAndCloseDecoder(decoder, ctx, data);
  }

  @Test
  public void testInterception() throws Exception {
    int interceptedReads = 3;
    TransportFrameDecoder decoder = new TransportFrameDecoder();
    TransportFrameDecoder.Interceptor interceptor = spy(new MockInterceptor(interceptedReads));
    ChannelHandlerContext ctx = mockChannelHandlerContext();

    byte[] data = new byte[8];
    ByteBuf len = Unpooled.copyLong(8 + data.length);
    ByteBuf dataBuf = Unpooled.wrappedBuffer(data);

    try {
      decoder.setInterceptor(interceptor);
      for (int i = 0; i < interceptedReads; i++) {
        decoder.channelRead(ctx, dataBuf);
        assertEquals(0, dataBuf.refCnt());
        dataBuf = Unpooled.wrappedBuffer(data);
      }
      decoder.channelRead(ctx, len);
      decoder.channelRead(ctx, dataBuf);
      verify(interceptor, times(interceptedReads)).handle(any(ByteBuf.class));
      verify(ctx).fireChannelRead(any(ByteBuf.class));
      assertEquals(0, len.refCnt());
      assertEquals(0, dataBuf.refCnt());
    } finally {
      release(len);
      release(dataBuf);
    }
  }

  @Test
  public void testRetainedFrames() throws Exception {
    TransportFrameDecoder decoder = new TransportFrameDecoder();

    AtomicInteger count = new AtomicInteger();
    List<ByteBuf> retained = new ArrayList<>();

    ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    when(ctx.fireChannelRead(any())).thenAnswer(in -> {
      // Retain a few frames but not others.
      ByteBuf buf = (ByteBuf) in.getArguments()[0];
      if (count.incrementAndGet() % 2 == 0) {
        retained.add(buf);
      } else {
        buf.release();
      }
      return null;
    });

    ByteBuf data = createAndFeedFrames(100, decoder, ctx);
    try {
      // Verify all retained buffers are readable.
      for (ByteBuf b : retained) {
        byte[] tmp = new byte[b.readableBytes()];
        b.readBytes(tmp);
        b.release();
      }
      verifyAndCloseDecoder(decoder, ctx, data);
    } finally {
      for (ByteBuf b : retained) {
        release(b);
      }
    }
  }

  @Test
  public void testSplitLengthField() throws Exception {
    byte[] frame = new byte[1024 * (RND.nextInt(31) + 1)];
    ByteBuf buf = Unpooled.buffer(frame.length + 8);
    buf.writeLong(frame.length + 8);
    buf.writeBytes(frame);

    TransportFrameDecoder decoder = new TransportFrameDecoder();
    ChannelHandlerContext ctx = mockChannelHandlerContext();
    try {
      decoder.channelRead(ctx, buf.readSlice(RND.nextInt(7)).retain());
      verify(ctx, never()).fireChannelRead(any(ByteBuf.class));
      decoder.channelRead(ctx, buf);
      verify(ctx).fireChannelRead(any(ByteBuf.class));
      assertEquals(0, buf.refCnt());
    } finally {
      decoder.channelInactive(ctx);
      release(buf);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeFrameSize() throws Exception {
    testInvalidFrame(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyFrame() throws Exception {
    // 8 because frame size includes the frame length.
    testInvalidFrame(8);
  }

  /**
   * Creates a number of randomly sized frames and feed them to the given decoder, verifying
   * that the frames were read.
   */
  private ByteBuf createAndFeedFrames(
      int frameCount,
      TransportFrameDecoder decoder,
      ChannelHandlerContext ctx) throws Exception {
    ByteBuf data = Unpooled.buffer();
    for (int i = 0; i < frameCount; i++) {
      byte[] frame = new byte[1024 * (RND.nextInt(31) + 1)];
      data.writeLong(frame.length + 8);
      data.writeBytes(frame);
    }

    try {
      while (data.isReadable()) {
        int size = RND.nextInt(4 * 1024) + 256;
        decoder.channelRead(ctx, data.readSlice(Math.min(data.readableBytes(), size)).retain());
      }

      verify(ctx, times(frameCount)).fireChannelRead(any(ByteBuf.class));
    } catch (Exception e) {
      release(data);
      throw e;
    }
    return data;
  }

  private void verifyAndCloseDecoder(
      TransportFrameDecoder decoder,
      ChannelHandlerContext ctx,
      ByteBuf data) throws Exception {
    try {
      decoder.channelInactive(ctx);
      assertTrue("There shouldn't be dangling references to the data.", data.release());
    } finally {
      release(data);
    }
  }

  private void testInvalidFrame(long size) throws Exception {
    TransportFrameDecoder decoder = new TransportFrameDecoder();
    ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    ByteBuf frame = Unpooled.copyLong(size);
    try {
      decoder.channelRead(ctx, frame);
    } finally {
      release(frame);
    }
  }

  private ChannelHandlerContext mockChannelHandlerContext() {
    ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    when(ctx.fireChannelRead(any())).thenAnswer(in -> {
      ByteBuf buf = (ByteBuf) in.getArguments()[0];
      buf.release();
      return null;
    });
    return ctx;
  }

  private void release(ByteBuf buf) {
    if (buf.refCnt() > 0) {
      buf.release(buf.refCnt());
    }
  }

  private static class MockInterceptor implements TransportFrameDecoder.Interceptor {

    private int remainingReads;

    MockInterceptor(int readCount) {
      this.remainingReads = readCount;
    }

    @Override
    public boolean handle(ByteBuf data) throws Exception {
      data.readerIndex(data.readerIndex() + data.readableBytes());
      assertFalse(data.isReadable());
      remainingReads -= 1;
      return remainingReads != 0;
    }

    @Override
    public void exceptionCaught(Throwable cause) throws Exception {

    }

    @Override
    public void channelInactive() throws Exception {

    }
  }
}
