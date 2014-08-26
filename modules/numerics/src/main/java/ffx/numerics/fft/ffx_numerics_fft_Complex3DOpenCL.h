/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ffx_numerics_fft_Complex3DOpenCL */

#ifndef _Included_ffx_numerics_fft_Complex3DOpenCL
#define _Included_ffx_numerics_fft_Complex3DOpenCL
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     ffx_numerics_fft_Complex3DOpenCL
 * Method:    setupNative
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_ffx_numerics_fft_Complex3DOpenCL_setupNative
  (JNIEnv *, jclass);

/*
 * Class:     ffx_numerics_fft_Complex3DOpenCL
 * Method:    createDefaultPlanNative
 * Signature: (JIIII)J
 */
JNIEXPORT jlong JNICALL Java_ffx_numerics_fft_Complex3DOpenCL_createDefaultPlanNative
  (JNIEnv *, jclass, jlong, jint, jint, jint, jint);

/*
 * Class:     ffx_numerics_fft_Complex3DOpenCL
 * Method:    setPlanPrecisionNative
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_ffx_numerics_fft_Complex3DOpenCL_setPlanPrecisionNative
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     ffx_numerics_fft_Complex3DOpenCL
 * Method:    setLayoutNative
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_ffx_numerics_fft_Complex3DOpenCL_setLayoutNative
  (JNIEnv *, jclass, jlong, jint, jint);

/*
 * Class:     ffx_numerics_fft_Complex3DOpenCL
 * Method:    executeTransformNative
 * Signature: (JIJJJ)I
 */
JNIEXPORT jint JNICALL Java_ffx_numerics_fft_Complex3DOpenCL_executeTransformNative
  (JNIEnv *, jclass, jlong, jint, jlong, jlong, jlong);

/*
 * Class:     ffx_numerics_fft_Complex3DOpenCL
 * Method:    destroyPlanNative
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_ffx_numerics_fft_Complex3DOpenCL_destroyPlanNative
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ffx_numerics_fft_Complex3DOpenCL
 * Method:    teardownNative
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_ffx_numerics_fft_Complex3DOpenCL_teardownNative
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
