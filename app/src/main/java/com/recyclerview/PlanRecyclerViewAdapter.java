package com.recyclerview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ais_ksgt_app.R;
import com.web_service.PObject;

import java.util.List;

public class PlanRecyclerViewAdapter extends RecyclerView.Adapter<PlanRecyclerViewAdapter.ViewHolder>{

    private LayoutInflater mInflater;
    private PlanRecyclerViewAdapter.ObjectClickListener mClickListener;

    // Позволяет отслеживать события нажатий
    public void setClickListener(PlanRecyclerViewAdapter.ObjectClickListener objectClickListener) {
        this.mClickListener = objectClickListener;
    }

    // Родительская активность реализует этот метод для ответа на события клика
    public interface ObjectClickListener {
        void onObjectClick(View view, int position);
    }

    private Context mContext;
    private List<PObject> adapterObjects;

    public PlanRecyclerViewAdapter(Context context, List objects)
    {

        this.mInflater = LayoutInflater.from(context);

        this.adapterObjects = objects;
        this.mContext = context;
    }

    @Override
        public PlanRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.cardview_object_row, parent, false);
        return new PlanRecyclerViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        holder.objectName.setText(adapterObjects.get(position).full_name);
        holder.objectStatus.setText("Имеются нарушения");
        holder.imageStatus.setImageResource(R.drawable.ic_cardview_delict);

        // Есть проверка и нарушения
        if(adapterObjects.get(position).is_checked && adapterObjects.get(position).delicts != null){
            holder.objectStatus.setText("Имеются нарушения");
            holder.imageStatus.setImageResource(R.drawable.ic_cardview_delict);
        }
        // Есть проверка и нет нарушений
        else if (adapterObjects.get(position).is_checked && adapterObjects.get(position).delicts == null){
            holder.objectStatus.setText("Нарушений не обнаружено");
            holder.imageStatus.setImageResource(R.drawable.ic_cardview_check);
        } else {
            holder.objectStatus.setText("Проверка не выполнена");
            holder.imageStatus.setImageResource(R.drawable.ic_cardview_uncheck);
        }
    }


    @Override
    public int getItemCount() {
        return adapterObjects.size();
    }

    // Сохраняет и перезаписывает представления при их прокрутке с экрана
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView imageStatus;
        TextView objectName;
        TextView objectStatus;
        RelativeLayout relativeLayout;

        ViewHolder(View itemView) {
            super(itemView);
            imageStatus = itemView.findViewById(R.id.imageStatus);
            objectName = itemView.findViewById(R.id.objectName);
            objectStatus = itemView.findViewById(R.id.objectStatus);
            relativeLayout = itemView.findViewById(R.id.relativeLayout);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onObjectClick(view, getAdapterPosition());
        }
    }
}

