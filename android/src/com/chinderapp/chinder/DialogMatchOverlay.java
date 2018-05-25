package com.chinderapp.chinder;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DialogMatchOverlay.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DialogMatchOverlay#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DialogMatchOverlay extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";

    private String mFromUrl;
    private String mToUrl;
    private String mName;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MatchOverlay.
     */
    // TODO: Rename and change types and number of parameters
    public static DialogMatchOverlay newInstance(String param1, String param2, String param3) {
        DialogMatchOverlay fragment = new DialogMatchOverlay();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putString(ARG_PARAM3, param3);
        fragment.setArguments(args);
        return fragment;
    }

    public DialogMatchOverlay() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFromUrl = getArguments().getString(ARG_PARAM1);
            mToUrl = getArguments().getString(ARG_PARAM2);
            mName = getArguments().getString(ARG_PARAM3);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // set background to null, giving us transparency
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
        getDialog().getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);

        View view = inflater.inflate(R.layout.fragment_match_overlay, container, false);
        Button chinMatchBtn = (Button)view.findViewById(R.id.chinMatchButton);
        chinMatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChinMatchButton(v);
            }
        });

        TextView chinMatchTitle = (TextView)view.findViewById(R.id.chinMatchTextView);
        chinMatchTitle.setText("You and " + mName + " fancy each other's chin");

        ImageView from = (ImageView)view.findViewById(R.id.chinMatchFromImageView);
        ImageView to = (ImageView)view.findViewById(R.id.chinMatchToImageView);
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheOnDisk(true) // default
                .build();
        ImageLoader.getInstance().displayImage(mFromUrl, from, options, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {}
        });
        ImageLoader.getInstance().displayImage(mToUrl, to, options, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {}
        });

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    public void onChinMatchButton(View v){
        getDialog().dismiss();
    }
}
