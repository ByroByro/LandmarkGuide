package zw.co.byrosolutions.landmarkguide.logic

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import zw.co.byrosolutions.landmarkguide.R
import zw.co.byrosolutions.landmarkguide.preferences.PreferenceProvider
import zw.co.byrosolutions.landmarkguide.retrofit.APIClient
import zw.co.byrosolutions.landmarkguide.retrofit.APIInterface
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class methods {
    companion object {
        // methods gives an alert to the user
        fun alertUser(title: String, message: String, ctx: Context) {
            val builder = AlertDialog.Builder(ctx)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton(R.string.dialog_ok) { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }

        // get date from time stamp
        fun getDateTime(s: String): String? {
            return try {
                val sdf = SimpleDateFormat("MM-dd-yyyy HH:mm:ss")
                val netDate = Date(s.toLong() * 1000)
                sdf.format(netDate)
            } catch (e: Exception) {
                e.toString()
            }
        }

        // get string date
        fun getStringDate(): String {
            var answer: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                current.format(formatter)
            } else {
                var date = Date()
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                formatter.format(date)
            }
            return answer
        }

        // copy string to clipboard
        fun copyTextToClipBoard(text: String, ctx: Context) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    val clipboard =
                        ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.text = text
                } else {
                    val clipboard =
                        ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied Text", text)
                    clipboard.setPrimaryClip(clip)
                }

                Toast.makeText(ctx, "Text copied to clip board", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                alertUser("Error copying", e.toString(), ctx)
            }
        }

        // paste or read string from clipboard
        fun pasteTextFromClipboard(ctx: Context): String {
            var textToPaste = ""
            try {
                val clipboard =
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE)) as? ClipboardManager
                textToPaste = clipboard?.primaryClip?.getItemAt(0)?.text.toString()
            } catch (e: Exception) {
                alertUser("Error getting data", e.toString(), ctx)
            }
            return textToPaste
        }

        fun saveSettings(mode: String, ctx: Context) {
            val dialog = ProgressDialog(ctx)
            try {

                val preferenceProvider = PreferenceProvider(ctx)

                var jsonObject = JSONObject()
                jsonObject.put("id", preferenceProvider.getUserId())
                jsonObject.put("email", preferenceProvider.getEmailId())
                jsonObject.put("mode", mode)
                if (mode == "landmark") {
                    jsonObject.put("value", preferenceProvider.getLandmark())
                } else {
                    jsonObject.put("value", preferenceProvider.getMetric())
                }

                val apiClient = APIClient().getInstance().create(
                    APIInterface::class.java
                )
                var attendance: Call<ResponseBody?>? =
                    apiClient.save_settings(jsonObject.toString())

                dialog.setTitle("Saving settings")
                dialog.setMessage("Uploading settings.Please wait...")
                dialog.setCanceledOnTouchOutside(false)
                dialog.show()

                attendance?.enqueue(object : Callback<ResponseBody?> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {
                        dialog.dismiss()
                        if (response.isSuccessful) {

                        } else {
                            Toast.makeText(ctx, "Request failed", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                        dialog.dismiss()
                        alertUser(
                            "Response",
                            "A network error occurred. The request failed, please try again after a moment!",
                            ctx
                        )
                    }

                })
            } catch (e: Exception) {
                dialog.dismiss()
                alertUser(
                    "Response",
                    "An unexpected error occurred during executing current operation.Please contact your admin(s)",
                    ctx
                )
            }
        }

        /**
         * Method to get distance in miles and convert into Kilometers
         * @param miles
         * @return
         */
        fun convertIntoKms(miles: Double): String {
            val dec = DecimalFormat("#,###.00")
            val ans = 1.609 * miles
            return dec.format(ans)
        }

        /**
         * Method to get distance in km and convert into miles
         * @param km
         * @return
         */
        fun convertIntoMiles(km: Double): Double {
            return km / 1.609
        }
    }
}