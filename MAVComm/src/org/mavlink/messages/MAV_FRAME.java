/**
 * Generated class : MAV_FRAME
 * DO NOT MODIFY!
 **/
package org.mavlink.messages;
/**
 * Interface MAV_FRAME
 * 
 **/
public interface MAV_FRAME {
    /**
     * Global coordinate frame, WGS84 coordinate system. First value / x: latitude, second value / y: longitude, third value / z: positive altitude over mean sea level (MSL).
     */
    public final static int MAV_FRAME_GLOBAL = 0;
    /**
     * Local coordinate frame, Z-down (x: north, y: east, z: down).
     */
    public final static int MAV_FRAME_LOCAL_NED = 1;
    /**
     * NOT a coordinate frame, indicates a mission command.
     */
    public final static int MAV_FRAME_MISSION = 2;
    /**
     * Global coordinate frame, WGS84 coordinate system, relative altitude over ground with respect to the home position. First value / x: latitude, second value / y: longitude, third value / z: positive altitude with 0 being at the altitude of the home location.
     */
    public final static int MAV_FRAME_GLOBAL_RELATIVE_ALT = 3;
    /**
     * Local coordinate frame, Z-up (x: east, y: north, z: up).
     */
    public final static int MAV_FRAME_LOCAL_ENU = 4;
    /**
     * Global coordinate frame, WGS84 coordinate system. First value / x: latitude in degrees*1.0e-7, second value / y: longitude in degrees*1.0e-7, third value / z: positive altitude over mean sea level (MSL).
     */
    public final static int MAV_FRAME_GLOBAL_INT = 5;
    /**
     * Global coordinate frame, WGS84 coordinate system, relative altitude over ground with respect to the home position. First value / x: latitude in degrees*10e-7, second value / y: longitude in degrees*10e-7, third value / z: positive altitude with 0 being at the altitude of the home location.
     */
    public final static int MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6;
    /**
     * Offset to the current local frame. Anything expressed in this frame should be added to the current local frame position.
     */
    public final static int MAV_FRAME_LOCAL_OFFSET_NED = 7;
    /**
     * Setpoint in body NED frame. This makes sense if all position control is externalized - e.g. useful to command 2 m/s^2 acceleration to the right.
     */
    public final static int MAV_FRAME_BODY_NED = 8;
    /**
     * Offset in body NED frame. This makes sense if adding setpoints to the current flight path, to avoid an obstacle - e.g. useful to command 2 m/s^2 acceleration to the east.
     */
    public final static int MAV_FRAME_BODY_OFFSET_NED = 9;
    /**
     * Global coordinate frame with above terrain level altitude. WGS84 coordinate system, relative altitude over terrain with respect to the waypoint coordinate. First value / x: latitude in degrees, second value / y: longitude in degrees, third value / z: positive altitude in meters with 0 being at ground level in terrain model.
     */
    public final static int MAV_FRAME_GLOBAL_TERRAIN_ALT = 10;
    /**
     * Global coordinate frame with above terrain level altitude. WGS84 coordinate system, relative altitude over terrain with respect to the waypoint coordinate. First value / x: latitude in degrees*10e-7, second value / y: longitude in degrees*10e-7, third value / z: positive altitude in meters with 0 being at ground level in terrain model.
     */
    public final static int MAV_FRAME_GLOBAL_TERRAIN_ALT_INT = 11;
    /**
     * Body fixed frame of reference, Z-down (x: forward, y: right, z: down).
     */
    public final static int MAV_FRAME_BODY_FRD = 12;
    /**
     * Body fixed frame of reference, Z-up (x: forward, y: left, z: up).
     */
    public final static int MAV_FRAME_BODY_FLU = 13;
    /**
     * Odometry local coordinate frame of data given by a motion capture system, Z-down (x: north, y: east, z: down).
     */
    public final static int MAV_FRAME_MOCAP_NED = 14;
    /**
     * Odometry local coordinate frame of data given by a motion capture system, Z-up (x: east, y: north, z: up).
     */
    public final static int MAV_FRAME_MOCAP_ENU = 15;
    /**
     * Odometry local coordinate frame of data given by a vision estimation system, Z-down (x: north, y: east, z: down).
     */
    public final static int MAV_FRAME_VISION_NED = 16;
    /**
     * Odometry local coordinate frame of data given by a vision estimation system, Z-up (x: east, y: north, z: up).
     */
    public final static int MAV_FRAME_VISION_ENU = 17;
    /**
     * Odometry local coordinate frame of data given by an estimator running onboard the vehicle, Z-down (x: north, y: east, z: down).
     */
    public final static int MAV_FRAME_ESTIM_NED = 18;
    /**
     * Odometry local coordinate frame of data given by an estimator running onboard the vehicle, Z-up (x: east, y: noth, z: up).
     */
    public final static int MAV_FRAME_ESTIM_ENU = 19;
}
