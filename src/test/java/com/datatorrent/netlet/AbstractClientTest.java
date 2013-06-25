/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datatorrent.netlet;

import com.datatorrent.netlet.AbstractClient;
import com.datatorrent.netlet.DefaultEventLoop;
import com.datatorrent.netlet.ServerTest.ServerImpl;

import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Chetan Narsude <chetan@datatorrent.com>
 */
public class AbstractClientTest
{
  public AbstractClientTest()
  {
  }

  @Override
  public String toString()
  {
    return "ClientTest{" + '}';
  }

  public class ClientImpl extends AbstractClient
  {
    public static final int BUFFER_CAPACITY = 8 * 1024 + 1;
    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_CAPACITY);
    boolean read;

    @Override
    public String toString()
    {
      return "ClientImpl{" + "buffer=" + buffer + ", read=" + read + '}';
    }

    @Override
    public ByteBuffer buffer()
    {
      return buffer;
    }

    @Override
    public void read(int len)
    {
      if (buffer.position() == buffer.capacity()) {
        buffer.flip();
        read = true;

        int i = 0;
        LongBuffer lb = buffer.asLongBuffer();
        while (lb.hasRemaining()) {
          Assert.assertEquals(i++, lb.get());
        }

        assert (i == BUFFER_CAPACITY / 8);
      }
    }

    @Override
    public void connected()
    {
    }

    @Override
    public void disconnected()
    {
    }

  }

  public void sendData()
  {
  }

  @Test
  @SuppressWarnings("SleepWhileInLoop")
  public void verifySendReceive() throws IOException, InterruptedException
  {
    ServerImpl si = new ServerImpl();
    ClientImpl ci = new ClientImpl();

    DefaultEventLoop el = new DefaultEventLoop("test");
    new Thread(el).start();

    el.start("localhost", 5033, si);
    el.connect(new InetSocketAddress("localhost", 5033), ci);

    ByteBuffer outboundBuffer = ByteBuffer.allocate(ClientImpl.BUFFER_CAPACITY);
    LongBuffer lb = outboundBuffer.asLongBuffer();

    int i = 0;
    while (lb.hasRemaining()) {
      lb.put(i++);
    }

    boolean odd = false;
    outboundBuffer.position(i * 8);
    while (outboundBuffer.hasRemaining()) {
      outboundBuffer.put((byte)(odd ? 0x55 : 0xaa));
      odd = !odd;
    }

    byte[] array = outboundBuffer.array();
    while (!ci.send(array, 0, array.length)) {
      sleep(5);
    }

    sleep(100);

    el.disconnect(ci);
    el.stop(si);
    el.stop();
    assert (ci.read);
  }

  private static final Logger logger = LoggerFactory.getLogger(AbstractClientTest.class);
}