#include "chisel3_internal_mlir_IR.h"

#include "mlir-c/IR.h"
#include "mlir-c/Support.h"

#define toJava(cls, obj, env)                                                  \
  (*env)->NewDirectByteBuffer(env, obj.ptr, sizeof(cls))

#define fromJava(cls, bb, env) *((cls *)(*env)->GetDirectBufferAddress(env, bb))

JNIEXPORT jlong JNICALL
Java_chisel3_internal_mlir_IR_mlirContextCreate(JNIEnv *env, jobject clazz) {
  MlirContext ctx = mlirContextCreate();
  mlirContextSetAllowUnregisteredDialects(ctx, true);
  return (long)ctx.ptr;
}

JNIEXPORT jlong JNICALL Java_chisel3_internal_mlir_IR_mlirLocationCreate(
    JNIEnv *env, jobject clazz, jlong ptr) {
  MlirContext ctx = {(void *)ptr};
  MlirLocation loc = mlirLocationUnknownGet(ctx);
  return (long)loc.ptr;
}

JNIEXPORT jlong JNICALL Java_chisel3_internal_mlir_IR_mlirModuleCreate(
    JNIEnv *env, jobject clazz, jlong ptr) {
  MlirLocation loc = {(void *)ptr};
  MlirModule mod = mlirModuleCreateEmpty(loc);
  return (long)mod.ptr;
}

JNIEXPORT void JNICALL Java_chisel3_internal_mlir_IR_mlirModulePrint(
    JNIEnv *env, jobject clazz, jlong ptr) {
  MlirModule mod = {(void *)ptr};
  MlirOperation op = mlirModuleGetOperation(mod);
  mlirOperationDump(op);
}

JNIEXPORT jlong JNICALL Java_chisel3_internal_mlir_IR_mlirOperationCreate(
    JNIEnv *env, jobject clazz, jlong modPtr, jlong locPtr, jstring name) {
  MlirModule mod = {(void *)modPtr};
  MlirLocation loc = {(void *)locPtr};
  jboolean isCopy = 1;
  const char *chars = (*env)->GetStringUTFChars(env, name, &isCopy);
  jsize size = (*env)->GetStringUTFLength(env, name);
  MlirStringRef str = mlirStringRefCreate(chars, size);
  MlirOperationState state = mlirOperationStateGet(str, loc);
  MlirOperation op = mlirOperationCreate(&state);
  MlirBlock body = mlirModuleGetBody(mod);
  mlirBlockAppendOwnedOperation(body, op);
  return (long)op.ptr;
}
