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

package com.comino.dev;

import java.util.Arrays;

import com.comino.msp.model.DataModel;
import com.comino.msp.slam.map2D.ILocalMap;
import com.comino.msp.slam.map2D.filter.ILocalMapFilter;
import com.comino.msp.utils.MSPArrayUtils;

import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F32;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.point.Vector4D_F64;

public class LocalMap2DGrayU8 implements ILocalMap {

	private static final long OBLIVISION_TIME_MS = 10000;
	private static final int  MAX_CERTAINITY     = 256;

	private GrayU8 		    map;
	private GrayU8		    	window;

	private int 				cell_size_mm;
	private float			center_x_mm;
	private float			center_y_mm;

	private float			local_x_mm;
	private float			local_y_mm;

	private int 			    map_dimension;
	private int             window_dimension;
	private long            tms;

	private int				threshold = 0;

	public LocalMap2DGrayU8() {
        this(40.0f,0.05f,2.0f,2);
	}

	public LocalMap2DGrayU8(float diameter_m, float cell_size_m, float window_diameter_m, int threshold) {
		this(diameter_m, cell_size_m, window_diameter_m, diameter_m/2f, diameter_m/2f, threshold );
	}

	public LocalMap2DGrayU8(float map_diameter_m, float cell_size_m, float window_diameter_m, float center_x_m, float center_y_m,  int threshold) {
		cell_size_mm = (int)(cell_size_m * 1000f);
		this.threshold = threshold;

		map_dimension  = (int)Math.floor(map_diameter_m / cell_size_m );
		map = new GrayU8(map_dimension,map_dimension);

		window_dimension = (int)Math.floor(window_diameter_m / cell_size_m );
		window = new GrayU8(window_dimension,window_dimension);

		reset();

		this.center_x_mm = center_x_m * 1000f;
		this.center_y_mm = center_y_m * 1000f;

		System.out.println("LocalMap2D initialized with "+map_dimension+"x"+map_dimension+" map and "+window_dimension+"x"+window_dimension+" window cells. ");
	}

	public void 	setLocalPosition(Vector3D_F32 point) {
		local_x_mm = point.x *1000f + center_x_mm;;
		local_y_mm = point.y *1000f + center_y_mm;;
	}

	public boolean update(Vector3D_F32 point) {
		return set(point.x, point.y,10);
	}

	public boolean update(Vector3D_F32 point, int incr) {
		return set(point.x, point.y,incr);
	}

	public boolean update(Point3D_F64 point, Vector4D_F64 pos) {
		return set((float)(point.x+pos.x), (float)(point.y+pos.y),10);
	}


	public boolean update(Point3D_F64 point) {
		return set((float)point.x, (float)point.y,10);
	}

	public boolean update(Point3D_F64 point, int incr) {
		return set((float)point.x, (float)point.y,incr);
	}

	public boolean update(float lpos_x, float lpos_y, Vector3D_F32 point) {
		return set(lpos_x+point.x, lpos_y+point.y,1);
	}

	@Override
	public GrayU16 getMap() {
      return null;
	}

	public short[][] get() {
		return null;
	}


	public void processWindow(float lpos_x, float lpos_y) {

		int px,py, new_x,new_y;
		int center = window_dimension/2;

		px = (int)Math.floor( (lpos_x * 1000.0f + center_x_mm) / cell_size_mm);
		py = (int)Math.floor( (lpos_y * 1000.0f + center_y_mm) / cell_size_mm);

		for (int y = 0; y < window_dimension; y++) {
			for (int x = 0; x < window_dimension; x++) {

				new_x = x + px - center;
				new_y = y + py - center;

				if (new_x < map_dimension && new_y < map_dimension && new_x >= 0 && new_y >= 0)
					window.set(x, y, map.get(new_x, new_y));
				else
					window.set(x, y, Short.MAX_VALUE);
			}
		}
	}

	public int getWindowValue(int x, int y) {
		return (int)window.get(x, y);
	}


	public float nearestDistance(float lpos_x, float lpos_y) {
		float distance = Float.MAX_VALUE, d;
		int center = window_dimension/2;

		for (int y = 0; y < window_dimension; y++) {
			for (int x = 0; x < window_dimension; x++) {
				if(window.get(x, y) <= threshold)
					continue;
				d = (float)Math.sqrt((x - center)*(x - center) + (y - center)*(y - center));
				if(d < distance)
					distance = d;
			}
		}
		return (distance * cell_size_mm + cell_size_mm/2) / 1000.0f;
	}

	public short get(float xpos, float ypos) {
		int x = (int)Math.floor((xpos*1000f+center_x_mm)/cell_size_mm);
		int y = (int)Math.floor((ypos*1000f+center_y_mm)/cell_size_mm);
		if(x >=0 && x < map_dimension && y >=0 && y < map_dimension)
			return (short)map.get(x, y);
		return -1;
	}

	public boolean set(float xpos, float ypos, int value) {
		int x = (int)Math.floor((xpos*1000f+center_x_mm)/cell_size_mm);
		int y = (int)Math.floor((ypos*1000f+center_y_mm)/cell_size_mm);
		if(x >=0 && x < map_dimension && y >=0 && y < map_dimension && map.get(x, y) < MAX_CERTAINITY ) {
				map.set(x, y, map.get(x, y)+value);
			return true;
		}
		return false;
	}

	public void toDataModel(DataModel model,  boolean debug) {
		for (int y = 0; y <map_dimension; y++) {
			for (int x = 0; x < map_dimension; x++) {
				if(map.get(x, y) > threshold)
					model.grid.setBlock((x*cell_size_mm-center_x_mm)/1000f,(y*cell_size_mm-center_y_mm)/1000f,0, true);
				else
					model.grid.setBlock((x*cell_size_mm-center_x_mm)/1000f,(y*cell_size_mm-center_y_mm)/1000f,0, false);
			}
		}
		if(debug)
			System.out.println(model.grid);
	}

	public void forget() {
		if((System.currentTimeMillis()-tms)>OBLIVISION_TIME_MS) {
			tms = System.currentTimeMillis();
			for (int i = 0; i < map_dimension; ++i)
				for (int j = 0; j < map_dimension; ++j)
					if(map.get(i, j) > 0)
						map.set(i, j, map.get(i,j)-1);
		}
	}

	public int getWindowDimension() {
		return window_dimension;
	}

	public int getMapDimension() {
		return map_dimension;
	}

	public int getCellSize_mm() {
		return cell_size_mm;
	}

	public void reset() {
		Arrays.fill(map.data, (byte)0);
		Arrays.fill(window.data, (byte)0);
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		for(int y=0; y<map_dimension; y++) {
			for(int x=0; x<map_dimension; x++) {
				if(Math.abs(local_x_mm - x * cell_size_mm)<cell_size_mm &&
						Math.abs(local_y_mm - y * cell_size_mm)<cell_size_mm)
					b.append("o ");
				else if(map.get(x,y)>0 && map.get(x,y)<10) {
					b.append(map.get(x,y)+" ");
				}
				else if(map.get(x,y)>9)
					b.append("X ");
				else
					b.append(". ");
			}
			b.append("\n");
		}
		b.append("\n");
		return b.toString();
	}

	public String windowToString() {
		StringBuilder b = new StringBuilder();
		for(int y=0; y<window_dimension; y++) {
			for(int x=0; x<window_dimension; x++) {
				if(window.get(x, y)>0) {
					b.append("X ");
				}
				else
					b.append(". ");
			}
			b.append("\n");
		}
		b.append("\n");
		return b.toString();
	}

	@Override
	public void applyMapFilter(ILocalMapFilter filter) {


	}

	@Override
	public boolean update(float lpos_x, float lpos_y, Point3D_F64 point) {
		return false;
	}

	@Override
	public void setDataModel(DataModel model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void toDataModel(boolean debug) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setIsLoaded(boolean loaded) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLoaded() {
		// TODO Auto-generated method stub
		return false;
	}
}
