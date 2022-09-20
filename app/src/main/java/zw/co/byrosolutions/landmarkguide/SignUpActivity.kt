package zw.co.byrosolutions.landmarkguide

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
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

class SignUpActivity : AppCompatActivity() {

    private lateinit var btnSignIn: Button // sign in button
    private lateinit var txtEmail: TextInputEditText // sign in email
    private lateinit var txtPassword: TextInputEditText // sign in password
    private lateinit var txtConfirmPassword: TextInputEditText // sign in password confirm
    private lateinit var mProgressDialog: ProgressDialog // progress dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // set UI
            setContentView(R.layout.activity_sign_up)

            btnSignIn = findViewById(R.id.btnSignIn)
            txtEmail = findViewById(R.id.textUsername)
            txtPassword = findViewById(R.id.textPassword)
            txtConfirmPassword = findViewById(R.id.textConfirmPassword)

            mProgressDialog = ProgressDialog(this)

            btnSignIn.setOnClickListener {
                val user = txtEmail.text.toString()
                val pass = txtPassword.text.toString()
                val confPass = txtConfirmPassword.text.toString()

                if (!Patterns.EMAIL_ADDRESS.matcher(user).matches()) {
                    Toast.makeText(this, "Enter a valid email.", Toast.LENGTH_LONG).show()
                } else {
                    if (user == "" || pass == "" || confPass == "") {
                        Toast.makeText(this, "Enter all details", Toast.LENGTH_LONG).show()
                    } else {
                        if (pass.length < 6 || confPass.length < 6) {
                            Toast.makeText(
                                this,
                                "Password cannot be less than 6 characters",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            if (pass != confPass) {
                                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_LONG)
                                    .show()
                            } else {
                                signUp(user, pass)
                            }
                        }
                    }
                }

            }

        } catch (e: Exception) {
            methods.alertUser("Error loading UI", e.toString(), this)
        }
    }

    private fun signUp(email: String, password: String) {
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
            mProgressDialog.setTitle("Signing up")
            mProgressDialog.setMessage("Signing up, Please wait ...")
            mProgressDialog.setCanceledOnTouchOutside(false)
            mProgressDialog.show()

            val apiClient = APIClient().getInstance().create(APIInterface::class.java)
            var authCall: Call<ResponseBody?>? = apiClient.sign_up(json.toString())

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

                        if (message == "Account already exist") {
                            methods.alertUser(
                                "Error",
                                "You have used an email that is already taken!",
                                this@SignUpActivity
                            )
                        } else if (message == "Data Inserted Successfully") {
                            methods.alertUser(
                                "Success",
                                "Successfully registered.",
                                this@SignUpActivity
                            )
                        } else if (message == "Data not Inserted") {
                            methods.alertUser(
                                "Error",
                                "Could not write data, try again after a moment!",
                                this@SignUpActivity
                            )
                        } else if (message == "Empty field detected") {
                            methods.alertUser(
                                "Error",
                                "Please enter all fields!",
                                this@SignUpActivity
                            )
                        } else {
                            methods.alertUser("Error", message, this@SignUpActivity)
                        }

                    } else {
                        Toast.makeText(this@SignUpActivity, "Request failed!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    // set show progress to false
                    mProgressDialog.dismiss()
                    methods.alertUser(
                        "Error",
                        "A network error occurred. The request failed, please try again after a moment!",
                        this@SignUpActivity
                    )
                }

            })
        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }
}