package zw.co.byrosolutions.landmarkguide

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import zw.co.byrosolutions.landmarkguide.logic.methods
import zw.co.byrosolutions.landmarkguide.preferences.PreferenceProvider
import zw.co.byrosolutions.landmarkguide.retrofit.APIClient
import zw.co.byrosolutions.landmarkguide.retrofit.APIInterface

class MainActivity : AppCompatActivity() {


    private lateinit var btnSignIn: Button // sign in button
    private lateinit var txtEmail: TextInputEditText // sign in email
    private lateinit var txtPassword: TextInputEditText // sign in password
    private lateinit var mProgressDialog: ProgressDialog // progress dialog
    private lateinit var textRegister: TextView // register textview
    private lateinit var preferenceProvider: PreferenceProvider // prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            preferenceProvider = PreferenceProvider(this)

            try {
                // check shared prefs if user already signed in
                if (preferenceProvider.getRememberMe()!!) {
                    // navigate to dashboard
                    val dashboard = Intent(this, MainMapActivity::class.java)
                    dashboard.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(dashboard)

                    // FINISH THIS ACTIVITY
                    finish()
                }
            } catch (e: java.lang.Exception) {
                methods.alertUser("Prefs Error", e.toString(), this)
            }

            // setup UI
            setContentView(R.layout.activity_main)

            btnSignIn = findViewById(R.id.btnLogin)
            txtEmail = findViewById(R.id.textUsername)
            txtPassword = findViewById(R.id.textPassword)
            textRegister = findViewById(R.id.txtRegister)

            mProgressDialog = ProgressDialog(this)


            textRegister.setOnClickListener {
                val reg = Intent(this, SignUpActivity::class.java)
                startActivity(reg)
            }

            btnSignIn.setOnClickListener {
                val email = txtEmail.text.toString()
                val password = txtPassword.text.toString()

                if (email == "" || password == "") {
                    Toast.makeText(this, "Enter all fields", Toast.LENGTH_LONG).show()
                } else {

                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Enter a valid email.", Toast.LENGTH_LONG).show()
                    } else {
                        if (password.length < 6 || password.length < 6) {
                            Toast.makeText(
                                this,
                                "Password cannot be less than 6 characters",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // login
                            login(email, password)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }

    // login method
    private fun login(email: String, password: String) {
        try {

            //json object with username and password
            var json = JSONObject()
            try {
                json?.put("email", email)
                json?.put("password", password)
            } catch (e: JSONException) {
                println(e)
            }

            // update show progress
            mProgressDialog.setTitle("Signing in")
            mProgressDialog.setMessage("Signing in, Please wait ...")
            mProgressDialog.setCanceledOnTouchOutside(false)
            mProgressDialog.show()

            val apiClient = APIClient().getInstance().create(APIInterface::class.java)
            var authCall: Call<ResponseBody?>? = apiClient.login(json.toString())

            authCall?.enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    // set show progress to false
                    mProgressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()?.string()
                        val data = JSONObject(result)
                        val message = data.getString("message")

                        if (message == "Login Successful") {
                            preferenceProvider.saveUserId(data.getString("id"))
                            preferenceProvider.saveRememberMe(true)
                            preferenceProvider.saveEmailId(data.getString("email"))

                            // navigate to dashboard
                            val dashboard = Intent(this@MainActivity, MainMapActivity::class.java)
                            dashboard.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(dashboard)

                        } else if (message == "Login Failed") {
                            methods.alertUser(
                                "Success",
                                "Login failed,please check your credentials.",
                                this@MainActivity
                            )
                        } else {
                            methods.alertUser("Error", message, this@MainActivity)
                        }

                    } else {
                        Toast.makeText(this@MainActivity, "Request failed!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    // set show progress to false
                    mProgressDialog.dismiss()
                    methods.alertUser(
                        "Error",
                        "A network error occurred. The request failed, please try again after a moment!",
                        this@MainActivity
                    )
                }

            })
        } catch (e: Exception) {
            mProgressDialog.dismiss()
            methods.alertUser("Error", e.toString(), this)
        }
    }
}