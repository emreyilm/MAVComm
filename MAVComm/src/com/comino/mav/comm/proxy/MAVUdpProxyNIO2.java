/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/


package com.comino.mav.comm.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import org.mavlink.MAVLinkReader;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.lquac.msg_heartbeat;

import com.comino.mav.comm.IMAVComm;
import com.comino.msp.main.control.listener.IMAVLinkListener;


public class MAVUdpProxyNIO2 implements IMAVLinkListener, Runnable {

	private SocketAddress 			bindPort = null;
	private SocketAddress 			peerPort;
	private DatagramChannel 		channel = null;

	private HashMap<Class<?>,List<IMAVLinkListener>> listeners = null;

	private MAVLinkReader reader;

	private Selector selector;

	private IMAVComm comm;

	private boolean 			isConnected = false;

	private ByteBuffer rxBuffer = ByteBuffer.allocate(32768);


	//	public MAVUdpProxy() {
	//		this("172.168.178.2",14550,"172.168.178.1",14555);
	//	}


	public MAVUdpProxyNIO2(String peerAddress, int pPort, String bindAddress, int bPort, IMAVComm comm) {

		peerPort = new InetSocketAddress(peerAddress, pPort);
		bindPort = new InetSocketAddress(bindAddress, bPort);
		reader = new MAVLinkReader(1);

		this.comm = comm;

		listeners = new HashMap<Class<?>,List<IMAVLinkListener>>();

		System.out.println("Proxy (NIO2): BindPort="+bPort+" PeerPort="+pPort+ " BufferSize: "+rxBuffer.capacity());

	}

	public boolean open() {

		if(channel!=null && channel.isConnected()) {
			isConnected = true;
			return true;
		}
		while(!isConnected) {
			try {

				isConnected = true;
				//			System.out.println("Connect to UDP channel");
				try {
					channel = DatagramChannel.open();
					channel.socket().bind(bindPort);
					channel.socket().setTrafficClass(0x10);
					channel.configureBlocking(false);

				} catch (IOException e) {
					continue;
				}
				channel.connect(peerPort);
				selector = Selector.open();

				Thread t = new Thread(this);
				t.setName("Proxy worker");
				t.start();

				return true;
			} catch(Exception e) {
				close();
				isConnected = false;

			}
		}
		return false;
	}

	public boolean isConnected() {
		return isConnected;
	}


	public void close() {
		isConnected = false;

		try {
			selector.close();
			if (channel != null) {
				channel.disconnect();
				channel.close();
			}
		} catch(Exception e) {

		}
	}

	public void registerListener(Class<?> clazz, IMAVLinkListener listener) {
		System.out.println("Register MavLink listener: "+clazz.getSimpleName()+" : "+listener.getClass().getName());
		List<IMAVLinkListener> list = null;
		if(listeners.containsKey(clazz)) {
			list = listeners.get(clazz);
			list.add(listener);
		} else {
			list  = new ArrayList<IMAVLinkListener>();
			list.add(listener);
			listeners.put(clazz, list);
		}
	}

	@Override
	public void run() {

		SelectionKey key = null;
		MAVLinkMessage msg = null;
		Iterator<?> selectedKeys = null;
		List<IMAVLinkListener> listener_list = null;

		try {
			channel.register(selector, SelectionKey.OP_READ );

			if(comm.isConnected()) {
				msg_heartbeat hb = new msg_heartbeat(255,1);
				hb.isValid = true;
				comm.write(hb);
			}


			while(isConnected) {

				if(selector.select(2000)==0)
					continue;

				selectedKeys = selector.selectedKeys().iterator();

				while (selectedKeys.hasNext()) {
					key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					if (key.isReadable()) {
						try {
							if(channel.isConnected() && channel.receive(rxBuffer)!=null) {
								msg = reader.getNextMessage(rxBuffer.array(), rxBuffer.position());
								rxBuffer.clear();
								if(msg!=null) {
									listener_list = listeners.get(msg.getClass());
									if(listener_list!=null) {
										for(IMAVLinkListener listener : listener_list)
										   listener.received(msg);
									}
									else {
										if(comm.isConnected()) {
											//											System.out.println("Execute: "+msg.toString());
											comm.write(msg);
										}
									}
								}
							}
						} catch(Exception io) { }
					}
				}
			}
		} catch(Exception e) {
			close();
			isConnected = false;
		}
	}


	public int getBadCRC() {
		return reader.getBadCRC();
	}

	public  void write(MAVLinkMessage msg) throws IOException {
		if(msg!=null && channel!=null && channel.isConnected()) {
			channel.write(ByteBuffer.wrap(msg.encode()));
		}

	}

	//	MAVLinkReader r = new MAVLinkReader(99, true);


	@Override
	public void received(Object o) {

		//TODO: Issue:msg_logging_data_acked cannot be sent out => ULOG over MAVComm does not work

		//		if( o instanceof msg_logging_data_acked) {
		//
		//			synchronized(this) {
		//			msg_logging_data_acked p = (msg_logging_data_acked) o;
		//			System.out.println(p);
		//			try {
		//			byte[] b = p.encode();
		//			System.out.println("ID: 77 CRC: MSG=267 "+MAVLinkReader.bytesToHex(b, b.length));
		//			MAVLinkMessage rec = r.getNextMessage(b, b.length);
		//			System.out.println(b.length+" : "+rec);
		//			} catch(Exception w) {
		//				w.printStackTrace();
		//			}
		//			return;
		//			}
		//		}


		try {
			write((MAVLinkMessage) o);
		} catch (IOException e) {

		}
	}

}