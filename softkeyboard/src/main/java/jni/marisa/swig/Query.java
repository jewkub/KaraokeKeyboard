/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.10
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package jni.marisa.swig;

public class Query {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected Query(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(Query obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        marisaJNI.delete_Query(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public String str() {
    return marisaJNI.Query_str(swigCPtr, this);
  }

  public long id() {
    return marisaJNI.Query_id(swigCPtr, this);
  }

}