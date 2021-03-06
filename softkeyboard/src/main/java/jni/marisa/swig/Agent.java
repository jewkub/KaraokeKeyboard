/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.10
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package jni.marisa.swig;

public class Agent {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected Agent(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(Agent obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        marisaJNI.delete_Agent(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public Agent() {
    this(marisaJNI.new_Agent(), true);
  }

  public void set_query(byte[] ptr) {
    marisaJNI.Agent_set_query__SWIG_0(swigCPtr, this, ptr);
  }

  public void set_query(long id) {
    marisaJNI.Agent_set_query__SWIG_1(swigCPtr, this, id);
  }

  public Key key() {
    return new Key(marisaJNI.Agent_key(swigCPtr, this), false);
  }

  public Query query() {
    return new Query(marisaJNI.Agent_query(swigCPtr, this), false);
  }

  public String key_str() {
    return marisaJNI.Agent_key_str(swigCPtr, this);
  }

  public long key_id() {
    return marisaJNI.Agent_key_id(swigCPtr, this);
  }

  public String query_str() {
    return marisaJNI.Agent_query_str(swigCPtr, this);
  }

  public long query_id() {
    return marisaJNI.Agent_query_id(swigCPtr, this);
  }

}
