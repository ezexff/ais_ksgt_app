package com.spinner;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ais_ksgt_app.R;
import com.web_service.Represent;

import java.util.List;

public class AdapterRepresentSpinner extends ArrayAdapter<Represent> { // Адаптер для Spinner с подразделениями

    private Context context;

    public AdapterRepresentSpinner(Context context, int resource, List<Represent> represents) {
        super(context, resource, represents);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = View.inflate(context, R.layout.spinner_represent, null);
            TextView tvCategory = convertView.findViewById(R.id.tv_represent);
            tvCategory.setText(this.getItem(position).label);
        }
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = View.inflate(context, R.layout.spinner_item, null);
        TextView tvCategoryName = view.findViewById(R.id.tv_category_name);

        tvCategoryName.setText(this.getItem(position).label);
        return view;
    }
}