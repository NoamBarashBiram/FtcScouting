package com.noam.ftcscouting.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class StaticSync {
    private static long id = 0;
    private static HashMap<Long, Notifiable> objects;

    public synchronized void sync(final Object message){
        for (Long id: objects.keySet()){
            final Notifiable object = objects.get(id);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    object.onNotified(message);
                }
            }).start();
        }
    }

    public long register(Notifiable object){
        objects.put(++id, object);
        return id;
    }

    public void unregister(Long id){
        objects.remove(id);
    }

    public interface Notifiable {
       void  onNotified(Object message);
    }
}
