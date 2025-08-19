package com.recyclerview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ais_ksgt_app.R;
import com.web_service.Delict;

import java.util.List;

public class DelictsRecyclerViewAdapterNew extends RecyclerView.Adapter<DelictsRecyclerViewAdapterNew.ViewHolder> {

    private LayoutInflater mInflater;
    private Context mContext;

    private List<Delict> adapterDelicts;

    public MyAdapterListener onClickListener;

    public interface MyAdapterListener {

        void itemViewOnClick(View v, int position);
        void dbtnDeleteViewOnClick(View v, int position);
    }

    public DelictsRecyclerViewAdapterNew(Context context, List<Delict> delicts, MyAdapterListener listener)
    {
        this.mInflater = LayoutInflater.from(context);

        this.adapterDelicts = delicts;
        this.mContext = context;

        onClickListener = listener;
    }

    public void setAdapterDelicts(List<Delict> delicts){
        this.adapterDelicts = delicts;
    }

    @Override
    public DelictsRecyclerViewAdapterNew.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.cardview_delict_row, parent, false);
        return new DelictsRecyclerViewAdapterNew.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DelictsRecyclerViewAdapterNew.ViewHolder holder, final int position) {
        holder.delictName.setText(adapterDelicts.get(position).defection_label);
    }

    @Override
    public int getItemCount() {
        return adapterDelicts.size();
    }

    // Сохраняет и перезаписывает представления при их прокрутке с экрана
    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView delictName;
        Button delictDelete;
        RelativeLayout relativeLayout;

        ViewHolder(View itemView) {
            super(itemView);

            delictName = itemView.findViewById(R.id.delictName);
            delictDelete = itemView.findViewById(R.id.dbtnDelete);
            relativeLayout = itemView.findViewById(R.id.relativeLayout);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.itemViewOnClick(v, getAdapterPosition());
                }
            });
            delictDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.dbtnDeleteViewOnClick(v, getAdapterPosition());
                }
            });
        }
    }
}
