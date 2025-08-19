package com.spinner;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ais_ksgt_app.R;
import com.web_service.Plan;

import java.util.List;

public class AdapterPlanSpinner extends ArrayAdapter<Plan> { // Адаптер для Spinner с планами

    private Context context;

    public AdapterPlanSpinner(Context context, int resource, List<Plan> plans) {
        super(context, resource, plans);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = View.inflate(context, R.layout.spinner_plan, null);
            TextView tv_plan = convertView.findViewById(R.id.tv_plan);
            tv_plan.setText(this.getItem(position).content);
        }
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = View.inflate(context, R.layout.spinner_item, null);
        TextView tvCategoryName = view.findViewById(R.id.tv_category_name);

        tvCategoryName.setText(this.getItem(position).content);
        return view;
    }
}
