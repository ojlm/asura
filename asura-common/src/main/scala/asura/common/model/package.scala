package asura.common

package object model {

  type BoolErrorTypeRes[T] = (Boolean, String, T)
  type BoolErrorRes = (Boolean, String)
}
