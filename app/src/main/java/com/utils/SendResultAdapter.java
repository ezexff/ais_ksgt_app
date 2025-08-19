package com.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.ais_ksgt_app.R;
import com.web_service.Delict;
import com.web_service.PObject;
import com.web_service.Plan;

import java.util.ArrayList;
import java.util.List;

public class SendResultAdapter extends BaseAdapter { // Адаптер для отправки результата
    Context ctx;
    LayoutInflater lInflater;
    List<Plan> plans;

    public SendResultAdapter(Context context, List<Plan> plans) {
        this.ctx = context;
        this.plans = plans;
        this.lInflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Кол-во элементов
    @Override
    public int getCount() {

        return plans.size();
    }

    // Элемент по позиции
    @Override
    public Object getItem(int position) {

        return plans.get(position);
    }

    // Id по позиции
    @Override
    public long getItemId(int position) {

        return position;
    }

    // Пункт списка
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // используем созданные, но не используемые view
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.listview_sendresult, parent, false);
        }

        Plan p = getProduct(position);

        CheckBox cbPlan = (CheckBox) view.findViewById(R.id.listview_sendresult_checkBox);
        // присваиваем чекбоксу обработчик
        cbPlan.setOnCheckedChangeListener(myCheckChangeList);
        // пишем позицию
        cbPlan.setTag(position);
        // заполняем данными из плана: выбран или нет
        cbPlan.setChecked(p.checkBoxIsChecked);

        // заполняем название плана
        cbPlan.setText(p.content);

        // Заполняем счётчики
        TextView tvObjectsSize = (TextView) view.findViewById(R.id.tvObjectsSize);
        TextView tvCheckedCount = (TextView) view.findViewById(R.id.tvCheckedCount);
        TextView tvDelictsCount = (TextView) view.findViewById(R.id.tvDelictsCount);
        TextView tvUncheckedCount = (TextView) view.findViewById(R.id.tvUncheckedCount);

        String objectsSize = Integer.toString(objectsSize(position));
        tvObjectsSize.setText(objectsSize);

        String checkedCount = Integer.toString(checkedCount(position));
        tvCheckedCount.setText(checkedCount);

        String delictsCount = Integer.toString(delictsCount(position));
        tvDelictsCount.setText(delictsCount);

        String uncheckedCount = Integer.toString(uncheckedCount(position));
        tvUncheckedCount.setText(uncheckedCount);

        return view;
    }

    // План по позиции
    public Plan getProduct(int position) {

        return ((Plan) getItem(position));
    }

    // Выбранные планы
    public List<Plan> getBox() {
        List<Plan> box = new ArrayList<Plan>();
        for (Plan p : plans) {
            // план отмечен
            if (p.checkBoxIsChecked)
                box.add(p);
        }
        return box;
    }

    public int objectsSize(int position) {
        Plan p = plans.get(position);
        return p.pobjects.size();
    }

    public int checkedCount(int position) {
        int Result = 0;
        Plan p = plans.get(position);
        for (PObject o : p.pobjects)
            if (o.is_checked && o.delicts == null) {
                Result++;
            }
        return Result;
    }

    public int delictsCount(int position) {
        int Result = 0;
        Plan p = plans.get(position);
        for (PObject o : p.pobjects)
            if (o.is_checked && o.delicts != null) {
                for(Delict d : o.delicts) {
                    Result++;
                }
            }
        return Result;
    }

    public int uncheckedCount(int position) {
        int Result = 0;
        Plan p = plans.get(position);
        for (PObject o : p.pobjects)
            if (!o.is_checked && o.delicts == null) {
                Result++;
            }
        return Result;
    }

    // Оработчик для чекбоксов
    public OnCheckedChangeListener myCheckChangeList = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            // Меняем данные плана (выбран или нет)
            getProduct((Integer) buttonView.getTag()).checkBoxIsChecked = isChecked;
        }
    };
}