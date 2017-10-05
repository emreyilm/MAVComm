package com.comino.msp.execution.autopilot.tracker;

import java.util.Map.Entry;
import java.util.TreeMap;

import com.comino.msp.model.DataModel;
import com.comino.msp.utils.MSP3DUtils;

import georegression.struct.point.Vector4D_F32;

public class WayPointTracker implements Runnable {

	private static final int MAX_WAYPOINTS     = 100;
	private static final int RATE_MS           = 100;

	private final TreeMap<Long,Vector4D_F32>  list;
	private final TreeMap<Long,Vector4D_F32>  freezed;
	private final DataModel                   model;

	private boolean                       enabled = false;

	public WayPointTracker(DataModel model) {
		this.list    = new TreeMap<Long,Vector4D_F32>();
		this.freezed = new TreeMap<Long,Vector4D_F32>();
		this.model = model;
		System.out.println("WaypointTracker initialized with length "+MAX_WAYPOINTS*RATE_MS / 1000 +" seconds");
	}

	public Entry<Long, Vector4D_F32> getWaypoint(long ago_ms) {
		return list.floorEntry(model.sys.getSynchronizedPX4Time_us()/1000-ago_ms);
	}

	public Entry<Long, Vector4D_F32> pollLastWaypoint() {
		if(list.size()>0)
			return list.pollLastEntry();
		return null;
	}

	public long freeze() {
		freezed.clear();
		if(list.size()>0) {
			freezed.putAll(list);
			return freezed.lastEntry().getKey();
		}
		return 0;
	}

	public void unfreeze() {
		list.clear();
		freezed.clear();
	}

	public Entry<Long, Vector4D_F32> pollLastFreezedWaypoint() {
		if(freezed.size()>0)
			return freezed.pollLastEntry();
		return null;
	}

	public TreeMap<Long, Vector4D_F32> getFreezed() {
		return freezed;
	}

	public void start() {
		if(!enabled) {
			enabled = true;
			new Thread(this).start();
		}
	}

	public void stop() {
		enabled = false;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getSize() {
		return list.size();
	}

	@Override
	public void run() {

		list.clear(); long tms = 0; long sleep_tms = 0; float distance = Float.MAX_VALUE;

		while(enabled) {
			tms = model.sys.getSynchronizedPX4Time_us()/1000;
			Vector4D_F32 waypoint = new Vector4D_F32(model.state.l_x, model.state.l_y, model.state.l_z, model.state.h);

			if(!list.isEmpty())
				distance = MSP3DUtils.distance3D(waypoint, list.lastEntry().getValue());

			if((distance > 0.1f || Float.isNaN(distance)) && freezed.isEmpty()) {
				if(list.size()>=MAX_WAYPOINTS)
					list.pollFirstEntry();
				list.put(tms, waypoint);
			}

			sleep_tms = RATE_MS - (model.sys.getSynchronizedPX4Time_us()/1000 - tms );
			tms = model.sys.getSynchronizedPX4Time_us();

			if(sleep_tms> 0 && enabled)
				try { Thread.sleep(sleep_tms); 	} catch (InterruptedException e) { }
		}
	}

	public String toString() {
		return list.toString();
	}


}