/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.mav.comm.highspeedserial;

import com.sun.jna.Library;
import com.sun.jna.Native;

interface DLibrary extends Library {
	DLibrary INSTANCE = (DLibrary) Native.loadLibrary("AMA0", DLibrary.class);

	int  openAMA0(int rate);
	void closeAMA0();

	void writeAMA0(ByteArrayByReference str, int length);
	int  readAMA0(ByteArrayByReference str, int length);

}

public class SerialAMA0 {

	private ByteArrayByReference buffer = new ByteArrayByReference(1000);

	public SerialAMA0() {


	}

	public void open() {
		DLibrary.INSTANCE.openAMA0(921600);
		//DLibrary.INSTANCE.openAMA0(57600);
		System.out.println("SerialAMA0 opened");
	}

	public void close() {
		DLibrary.INSTANCE.closeAMA0();
	}


	public void writeBytes(byte[] buffer) {
		DLibrary.INSTANCE.writeAMA0(new ByteArrayByReference(buffer), buffer.length);
	}

	public int getInputBufferBytesCount() {
		int count = DLibrary.INSTANCE.readAMA0(buffer, buffer.length());
		return count;
	}

	public byte[] readBytes(int n) {
		return buffer.getValue(n);

	}

	public static void main(String[] args) {

		System.out.println("SerialAMA0 Test");

		SerialAMA0 ama0 = new SerialAMA0();
		ama0.open();

		byte[] s = {  (byte)1, (byte)8  };

		ama0.writeBytes(s);
//		while(true) {
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			byte[] b =ama0.readBytes(ama0.getInputBufferBytesCount());
			System.out.print(" bytes received => ");
			for(int i=0; i<b.length;i++)
				System.out.print(" "+b[i]);
			System.out.println();
//		}


	}

}
