package com.merakiphi.dprbillboard;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anuragmaravi on 06/02/17.
 */
public class ViewBillboardAdapter extends RecyclerView.Adapter<ViewBillboardAdapter.ViewHolder> {

	private List<BillBoard> mDataset = new ArrayList<>();
	private Context context;

	public static class ViewHolder extends RecyclerView.ViewHolder {
		public TextView textViewVendor, textViewDistrict, textViewLatitude, textViewLongitude, textViewStatus;
		public ImageView imageView;
		public ViewHolder(View v) {
			super(v);
			textViewVendor = (TextView) v.findViewById(R.id.textViewVendor);
			textViewDistrict = (TextView) v.findViewById(R.id.textViewDistrict);
			textViewLatitude = (TextView) v.findViewById(R.id.textViewLatitude);
			textViewLongitude = (TextView) v.findViewById(R.id.textViewLongitude);
			textViewStatus = (TextView) v.findViewById(R.id.textViewStatus);
			imageView = (ImageView) v.findViewById(R.id.imageView);
		}
	}

	public ViewBillboardAdapter(List<BillBoard> dataset, Context context) {
		mDataset.clear();
		mDataset = dataset;
		this.context = context;
	}

	@Override
	public ViewBillboardAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view, parent, false);
		ViewHolder vh = new ViewHolder(v);
		return vh;
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		final BillBoard gymList = mDataset.get(position);
		holder.textViewVendor.setText(gymList.getType());
		holder.textViewDistrict.setText(gymList.getDistrict());
		holder.textViewLatitude.setText(gymList.getLatitude());
		holder.textViewLongitude.setText(gymList.getLongitude());
		holder.textViewStatus.setText(gymList.getHeight());

		Glide.with(context).load(gymList.getImage()).placeholder(R.drawable.hoarding).dontTransform().diskCacheStrategy(DiskCacheStrategy.NONE).into(holder.imageView);
//		Picasso.with(context).load(gymList.getImage())
//				.fit()
//				.centerCrop().into(holder.imageView);
		holder.itemView.setOnClickListener(new View.OnClickListener() {
	@Override
	public void onClick(View v) {
		Intent intent = new Intent(context, UpdateBillboardActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("key", gymList.getKey());
		context.startActivity(intent);
	}
});
	}

	@Override
	public int getItemCount() {
		return mDataset.size();
	}

}
