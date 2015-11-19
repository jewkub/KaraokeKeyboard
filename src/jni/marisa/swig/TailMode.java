/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.10
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package jni.marisa.swig;

public final class TailMode {
  public final static TailMode TEXT_TAIL = new TailMode("TEXT_TAIL", marisaJNI.TEXT_TAIL_get());
  public final static TailMode BINARY_TAIL = new TailMode("BINARY_TAIL", marisaJNI.BINARY_TAIL_get());
  public final static TailMode DEFAULT_TAIL = new TailMode("DEFAULT_TAIL", marisaJNI.DEFAULT_TAIL_get());

  public final int swigValue() {
    return swigValue;
  }

  public String toString() {
    return swigName;
  }

  public static TailMode swigToEnum(int swigValue) {
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (int i = 0; i < swigValues.length; i++)
      if (swigValues[i].swigValue == swigValue)
        return swigValues[i];
    throw new IllegalArgumentException("No enum " + TailMode.class + " with value " + swigValue);
  }

  private TailMode(String swigName) {
    this.swigName = swigName;
    this.swigValue = swigNext++;
  }

  private TailMode(String swigName, int swigValue) {
    this.swigName = swigName;
    this.swigValue = swigValue;
    swigNext = swigValue+1;
  }

  private TailMode(String swigName, TailMode swigEnum) {
    this.swigName = swigName;
    this.swigValue = swigEnum.swigValue;
    swigNext = this.swigValue+1;
  }

  private static TailMode[] swigValues = { TEXT_TAIL, BINARY_TAIL, DEFAULT_TAIL };
  private static int swigNext = 0;
  private final int swigValue;
  private final String swigName;
}

