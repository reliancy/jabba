package com.reliancy.util;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/** Least recently used cache is a useful map of sorts. 
 * It has a fixed capacity and it forgets least used entries if new are added.
 * If an allocator is installed it is consulted on cache miss.
 * If a disposer is installed is is consulted on cache overflow.
 * We can provide the same object that implements both and make use of a pool.
*/
public class LRUCache<K,V>{
    public static interface Allocator<K,V>{
        V request(K key);
    }
    public static interface Disposer<K,V>{
        void release(K key,V val);
    }
    final Map<K,V> data;
    int capacity;
    final LinkedList<K> order=new LinkedList<>();
    Allocator<K,V> allocator;
    Disposer<K,V> disposer;

    public LRUCache(int capacity,Map<K,V> backend){
        this.capacity=capacity;
        data=backend!=null?backend:new HashMap<K,V>();
    }
    public LRUCache(int capacity){
        this(capacity,null);
    }
    public LRUCache<K,V> setAllocator(Allocator<K,V> a){
        allocator=a;
        return this;
    }
    public LRUCache<K,V> setDisposer(Disposer<K,V> a){
        disposer=a;
        return this;
    }
    public int size() {
        return data.size();
    }
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }
    public V get(K key) {
        V ret=data.get(key);
        if(ret!=null){
            //cache is hit
            order.remove(key);
            order.addFirst(key);
        }else{
            //cache is missed
            ret=allocator!=null?allocator.request(key):null;
        }
        return ret;
    }
    public V put(K key, V value) {
        if(order.size()>=capacity){
            // capacity is reached
            K last=order.removeLast();
            data.remove(last);
            if(disposer!=null) disposer.release(key, value);
        }
        order.addFirst(key);
        return data.put(key,value);
    }
    public V remove(Object key) {
        order.remove(key);
        return data.remove(key);
    }
    public void clear() {
        order.clear();
        data.clear();
    }
}
