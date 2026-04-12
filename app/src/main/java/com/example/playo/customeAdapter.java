package com.example.playo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * CONCEPT: An Adapter is like a "Translator". 
 * It takes your data (ArrayList of Turfname) and translates it into 
 * visual rows (itemelayout.xml) that the ListView can display.
 hiiiiiiiii
 */
public class customeAdapter extends ArrayAdapter<Turfname> {

    /**
     * CONSTRUCTOR: This runs when you create the adapter in MainActivity.
     * @param turfnameArrayList: The data list.
     * @param context: The "Activity" where the list is living.
     */
    public customeAdapter(ArrayList<Turfname> turfnameArrayList, Context context) {
        // super() tells the parent (ArrayAdapter) which layout to use for each row
        // It also stores the context and list internally.
        super(context, R.layout.itemelayout, turfnameArrayList);
    }

    /**
     * PERFORMANCE TIP: The "ViewHolder" pattern.
     * Finding views using 'findViewById' is slow. We use this class to "cache" 
     * or remember the views so we don't have to search for them every time a row scrolls.
     */
    private static class MyViewHolder {
        ImageView image;
        TextView name;
        TextView price;
        RatingBar ratingBar;
    }

    /**
     * GETVIEW: This is the most important method! 
     * It is called every time a row is about to appear on the screen.
     * 
     * @param position: Which item in the list are we drawing? (0, 1, 2...)
     * @param convertView: A recycled row. Android reuses old rows to save memory!
     * @param parent: The ListView itself.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        
        // 1. Get the specific data object for this row
        Turfname turfname = getItem(position);
        
        // This will hold our view references
        MyViewHolder viewHolder;

        // 2. RECYCLING CHECK:
        // If convertView is null, it means we need to create a brand new row.
        if (convertView == null) {
            viewHolder = new MyViewHolder();
            
            // LayoutInflater takes your XML (itemelayout.xml) and turns it into a Java View object
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.itemelayout, parent, false);

            // 3. LINKING: Connect the variables in our Holder to the actual IDs in XML
            viewHolder.name = convertView.findViewById(R.id.turfname);
            viewHolder.price = convertView.findViewById(R.id.price);
            viewHolder.image = convertView.findViewById(R.id.image);
            viewHolder.ratingBar = convertView.findViewById(R.id.ratingBar);

            // "Tag" the view with the holder so we can find it later
            convertView.setTag(viewHolder);
        } else {
            // If convertView is NOT null, we are REUSING an old row. 
            // We just grab the holder we saved earlier. This is very fast!
            viewHolder = (MyViewHolder) convertView.getTag();
        }

        // 4. BINDING: Now that we have the row, put the actual data into the views
        if (turfname != null) {
            viewHolder.name.setText(turfname.getName());
            // Converting double price to String so it can be displayed in a TextView
            viewHolder.price.setText(String.valueOf(turfname.getPrice()));
            // Setting the image using the resource ID (like R.drawable.my_image)
            viewHolder.image.setImageResource(turfname.getTurfimg());
            // RatingBar expects a float value
            viewHolder.ratingBar.setRating((float) turfname.getRating());
        }

        // 5. FINISHED: Return the completed view to render on screen
        return convertView;
    }
}
