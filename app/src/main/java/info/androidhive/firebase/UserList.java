package info.androidhive.firebase;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class UserList extends ArrayAdapter<User> {

   private Activity context;
   private List<User> userList;

   public UserList(Activity context, List<User>userList){

       super(context,R.layout.list_layout,userList);
       this.context=context;
       this.userList=userList;
   }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        LayoutInflater inflater = context.getLayoutInflater();
        View listViewItem = inflater.inflate(R.layout.list_layout,null,true);
        TextView textViewName=(TextView) listViewItem.findViewById(R.id.name);
        TextView textViewTokenId=(TextView) listViewItem.findViewById(R.id.tokenid);
        User user = userList.get(position);

        textViewName.setText(user.getName());
        textViewTokenId.setText(user.getRefreshedToken());

       return listViewItem;
    }

}
/* // ListView on item selected listener.
 listView.setOnItemClickListener(new OnItemClickListener()
 {

 @Override
 public void onItemClick(AdapterView<?> parent, View view,
 int position, long id) {
 // TODO Auto-generated method stub
 Toast.makeText(MainActivity.this, listValue[position], Toast.LENGTH_SHORT).show();
 }
 });*/