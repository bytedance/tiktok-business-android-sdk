package com.example;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class PropEditor extends AppCompatActivity {

    private ListView myList;
    private ArrayList<Property> props;
    private PropListAdapter adapter;
    private Integer nextPropID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prop_editor);

        getSupportActionBar().setTitle("Property Editor");

        myList = (ListView) findViewById(R.id.list);
        ImageButton fabImageButton = (ImageButton) findViewById(R.id.fab_image_button);

        props = new ArrayList<Property>();
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                if (bundle.get(key) != null) {
                    nextPropID++;
                    props.add(new Property(nextPropID.toString(), key, (String) bundle.get(key)));
                }
            }
        }

        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        (ListView) findViewById(R.id.list),
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int id : reverseSortedPositions) {
                                    Property pty = (Property) adapter.getItem(id);
                                    if (pty != null) {
                                        props.remove(pty);
                                    }
                                }
                                updateListUI();
                            }
                        });
        findViewById(R.id.list).setOnTouchListener(touchListener);

        fabImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.notifyDataSetChanged();
                // form to input prop name and value
                AlertDialog.Builder propBuilder = new AlertDialog.Builder(PropEditor.this);
                propBuilder.setTitle("Add a new Property");
                propBuilder.setMessage("Enter property name and value");

                LinearLayout layout = new LinearLayout(PropEditor.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                lp.setMargins(40, 0, 40, 0);
                final EditText nameBox = new EditText(PropEditor.this);
                nameBox.setHint("Prop Name");
                nameBox.setLayoutParams(lp);
                layout.addView(nameBox);
                final EditText valueBox = new EditText(PropEditor.this);
                valueBox.setHint("Prop Value");
                valueBox.setLayoutParams(lp);
                layout.addView(valueBox);
                propBuilder.setView(layout);
                propBuilder.setPositiveButton("Add Prop", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        nextPropID++;
                        props.add(new Property(nextPropID.toString(), nameBox.getText().toString(), valueBox.getText().toString()));
                    }
                });
                propBuilder.setNegativeButton("Cancel", null);
                propBuilder.create().show();
            }
        });
        updateListUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.prop_action_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.done_btn) {
            Intent intent= new Intent();
            Bundle bundlePros = new Bundle();
            for (Property p : props) {
                bundlePros.putString(p.key, p.value);
            }
            intent.putExtras(bundlePros);
            setResult(2, intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onDeleteButtonClick(View view) {
        View v = (View) view.getParent();
        TextView itemID = (TextView) v.findViewById(R.id.item_id);
        String itemString = itemID.getText().toString();
        for (Property p : props) {
            if (p._id.equals(itemString)) {
                props.remove(p);
                updateListUI();
                break;
            }
        }
    }

    private void updateListUI() {
        adapter = new PropListAdapter(this, props);
        myList.setAdapter(adapter);
    }

    public void onEditButtonClick(View view) {
        adapter.notifyDataSetChanged();
        View v = (View) view.getParent();
        TextView itemID = (TextView) v.findViewById(R.id.item_id);
        String itemString = itemID.getText().toString();
        Property property = null;
        for (Property p : props) {
            if (p._id.equals(itemString)) {
                property = p;
                break;
            }
        }
        // form to input prop name and value
        AlertDialog.Builder propBuilder = new AlertDialog.Builder(PropEditor.this);
        propBuilder.setTitle("Edit Property");
        propBuilder.setMessage("Enter property name and value");

        LinearLayout layout = new LinearLayout(PropEditor.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(40, 0, 40, 0);
        final EditText nameBox = new EditText(PropEditor.this);
        nameBox.setHint("Prop Name");
        nameBox.setText(property.key);
        nameBox.setLayoutParams(lp);
        layout.addView(nameBox);
        final EditText valueBox = new EditText(PropEditor.this);
        valueBox.setHint("Prop Value");
        valueBox.setText(property.value);
        valueBox.setLayoutParams(lp);
        layout.addView(valueBox);
        propBuilder.setView(layout);
        final Property finalProperty = property;
        propBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                props.set(props.indexOf(finalProperty), new Property(finalProperty._id, nameBox.getText().toString(), valueBox.getText().toString()));
            }
        });
        propBuilder.setNegativeButton("Cancel", null);
        propBuilder.create().show();
    }
}