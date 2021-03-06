package com.example.s1.menuui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.comment_list_item.*
import kotlinx.android.synthetic.main.comment_list_item.view.*
import kotlinx.android.synthetic.main.item_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*
import org.w3c.dom.Comment

class CommentActivity : AppCompatActivity() {

    var contentUid : String? = null
    var destinationUid : String? = null
    var collection : String? = null
    val firestore = FirebaseFirestore.getInstance()
    val TAG = "Comment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        val contentProfileIv = findViewById<ImageView>(R.id.comment_content_profile_iv)
        val contentProfileTv = findViewById<TextView>(R.id.comment_profile_tv)
        val contentTv = findViewById<TextView>(R.id.comment_content_tv)
        val toolbar = findViewById<Toolbar>(R.id.comment_toolbar)
        val contentUserId = intent.getStringExtra("contentUserId")
        val contentProfileUrl = intent.getStringExtra("contentProfileUrl")
        val content = intent.getStringExtra("content")

        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")
        collection = intent.getStringExtra("collection")

        // set content profile image
//        Glide.with(this).load(contentProfileUrl).apply(RequestOptions().circleCrop()).into(contentProfileIv)

        FirebaseFirestore.getInstance().collection("profileImages")?.document(destinationUid!!)?.addSnapshotListener { documentSnapshot, exception ->
            if(documentSnapshot == null) return@addSnapshotListener
            if(documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]
                Glide.with(this).load(url).apply(RequestOptions().circleCrop()).into(contentProfileIv)
            }
        }

        // set content explain
        contentProfileTv.text = contentUserId
        contentTv.text = content

        setSupportActionBar(toolbar)
        val ab = supportActionBar!!
        ab.setDisplayShowTitleEnabled(false)
        ab.setDisplayHomeAsUpEnabled(true)

        val commentRecyclerviewAdapter = CommentRecyclerviewAdapter()
        comment_recyclerview.adapter = commentRecyclerviewAdapter
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        comment_btn_send?.setOnClickListener {
            var comment = CommentDTO()
            val auth = FirebaseAuth.getInstance()

            comment.userId = auth.currentUser?.email
            comment.uid = auth.currentUser?.uid
            comment.comment = comment_edit_message.text.toString()
            comment.timestamp = System.currentTimeMillis()

            firestore.collection(collection!!).document(contentUid!!).collection("comments").document().set(comment)
            commentAlarm(destinationUid!!,comment_edit_message.text.toString())
            commentRecyclerviewAdapter.comments.add(comment)
            comment_edit_message.setText("")
            commentRecyclerviewAdapter.notifyDataSetChanged()
        }
    }

    fun commentAlarm(destinationUid : String, message : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.kind = 1
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
    }

    inner class CommentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

//        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()
        var comments : ArrayList<CommentDTO> = arrayListOf()
        var cmtProfileImgUrl : HashMap<String, String> = hashMapOf()

        init {
                firestore.collection(collection!!)
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()
                    cmtProfileImgUrl.clear()

                    if(querySnapshot == null)return@addSnapshotListener

                    for(snapshot in querySnapshot.documents){
                        var cmt = snapshot.toObject(CommentDTO::class.java)
                        comments.add(cmt!!)
                        if (!cmtProfileImgUrl.contains(cmt.uid)) {
                            firestore.collection("profileImages")?.document(cmt.uid!!)?.addSnapshotListener { documentSnapshot, exception ->
                                cmtProfileImgUrl.put(cmt.uid!!, documentSnapshot?.data!!["image"].toString())
                            }
                        }
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
//            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_comment,p0,false)
            var view = LayoutInflater.from(p0.context).inflate(R.layout.comment_list_item, p0,false)

            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var view = (p0 as CustomViewHolder).itemView

//            Glide.with(p0.itemView.context).load(cmtProfileImgUrl.get(comments[p1].uid)).apply(RequestOptions().circleCrop()).into(view.comment_profile_image)
            view.comment_id_tv.text = comments[p1].userId
            view.comment_contents_tv.text = comments[p1].comment

            firestore?.collection("profileImages")?.document(comments[p1].uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot == null) return@addSnapshotListener
                if(documentSnapshot.data != null){
                    var url = documentSnapshot?.data!!["image"]
                    if (url != null) {
                        Glide.with(view).load(url).apply(RequestOptions().circleCrop())
                            .into(view.comment_profile_image)
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}