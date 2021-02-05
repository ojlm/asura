package asura.ui.model

/** e.g:
 * {
 * "product": "OnePlus7TPro_CH",
 * "density": 3.5,
 * "host": "172.19.204.94",
 * "port": 8888,
 * "systemVersion": "Android 10",
 * "displaySize": "1440*2924",
 * "mac": "BA:25:8C:AA:C7:81",
 * "manufacturer": "OnePlus",
 * "densityDpi": 560,
 * "cpuABI": "arm64-v8a",
 * "screenSize": "1440*3120",
 * "serial": "bb8695f8",
 * "model": "HD1910",
 * "sdkVersion": 29,
 * "brand": "OnePlus",
 * "ram": 8
 * }
 */
case class MobileDriverInfo(
                             host: String,
                             port: Int,
                             systemVersion: String,
                             model: String,
                             brand: String,
                             manufacturer: String,
                             product: String,
                             sdkVersion: Int,
                             serial: String,
                             screenSize: String,
                             displaySize: String,
                             densityDpi: Int,
                             density: Float,
                             cpuABI: String,
                             mac: String,
                             ram: Int,
                           ) extends DriverInfo {

}
