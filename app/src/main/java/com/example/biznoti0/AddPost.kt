package com.example.biznoti0

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.*


import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.Navigation.findNavController
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_post.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_profile.*
import java.io.IOException
import java.util.*

class AddPost : AppCompatActivity() {
    private var firebaseStore: FirebaseStorage? = null
    private var storageReference: StorageReference? = null
    private var filePath: Uri? = null
    private lateinit var firebaseuser: FirebaseUser
    private lateinit var mFireAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)
        choose.setOnClickListener {
            //check runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_DENIED
                ) {
                    //permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                    //show popup to request runtime permission
                    requestPermissions(permissions, PERMISSION_CODE);
                } else {
                    //permission already granted
                    pickImageFromGallery();
                }
            } else {
                //system OS is < Marshmallow
                pickImageFromGallery();
            }
        }
        image.setOnClickListener {
            uploadImage()
        }

    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    companion object {
        //image pick code
        val IMAGE_PICK_CODE = 1000;

        //Permission code
        private val PERMISSION_CODE = 1001;
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            if (data != null && data.data != null) {

                filePath = data.data

                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath);

                    image_view.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }



        }

    }


    private fun uploadImage() {
        if (filePath == null) return


        val filename = UUID.randomUUID().toString()

        // get the location of your firebase storage by giving it the name of the directory you use
        // in our case we use the "user Info"
        val ref = FirebaseStorage.getInstance().getReference("/userStorage/$filename")

        ref.putFile(filePath!!)
            .addOnSuccessListener {
                Log.d("ProfileFragment", "Successfully uploaded image: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d("ProfileFragment", "File Download URL Location: $it")
                    Toast.makeText(this, "File Uploaded successfully", Toast.LENGTH_LONG).show()
                    uploadDatabase(it.toString())


                }
            }
            .addOnFailureListener {
                Log.d("ProfileFragment", "Failed to upload image to storage: ${it.message}")
            }
    }

    private fun uploadDatabase(uri: String)
    {
        val ImagePostId = UUID.randomUUID().toString()
        //var curruserId = mFireAuth.currentUser!!.uid
        // userreference = FirebaseDatabase.getInstance().reference.child("usersID").child(curruserId)
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/ImagePosts/$ImagePostId")
        val postId = ref.push().key
        val postMap = HashMap<String, Any>()
        postMap["postid"] = postId!!
        postMap["publisher"] = FirebaseAuth.getInstance().currentUser!!.uid
        postMap["postimage"] = uri

        ref.setValue(postMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Image has been Posted", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@AddPost, MainActivity::class.java))
                //findNavController().navigate(R.id.navigation_home)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Image cannot be Posted: ERROR", Toast.LENGTH_LONG).show()

            }

    }




}


