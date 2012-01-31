package com.codeslap.persistence;

import android.content.Context;

import java.util.List;

class QuickSqlAdapter implements SqlAdapter {

    private final Context mContext;
    private final String mDbName;

    QuickSqlAdapter(Context context, String dbName) {
        mContext = context;
        mDbName = dbName;
    }

    @Override
    public <T> Object store(T object) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        Object id = adapter.store(object);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    @Override
    public <T, G> Object store(T object, G attachedTo) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        Object id = adapter.store(object, attachedTo);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    @Override
    public <T> void storeCollection(List<T> collection) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        adapter.storeCollection(collection);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T> void storeUniqueCollection(List<T> collection) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        adapter.storeUniqueCollection(collection);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T> int update(T object, T where) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int count = adapter.update(object, where);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public <T> int update(T object, String where, String[] whereArgs) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int count = adapter.update(object, where, whereArgs);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public <T> T findFirst(T where) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        T result = adapter.findFirst(where);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public <T> T findFirst(Class<T> clazz, String where, String[] whereArgs) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        T result = adapter.findFirst(clazz, where, whereArgs);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = adapter.findAll(clazz);
        try {
            adapter.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return feeds;
    }

    @Override
    public <T> List<T> findAll(T where) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = adapter.findAll(where);
        try {
            adapter.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return feeds;
    }

    @Override
    public <T> List<T> findAll(T where, Constraint constraint) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = adapter.findAll(where, constraint);
        try {
            adapter.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return feeds;
    }

    @Override
    public <T, G> List<T> findAll(T where, G attachedTo) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = adapter.findAll(where, attachedTo);
        try {
            adapter.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return feeds;
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz, String where, String[] whereArgs) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        List<T> feeds = adapter.findAll(clazz, where, whereArgs);
        try {
            adapter.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return feeds;
    }

    @Override
    public <T> int delete(T where) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int delete = adapter.delete(where);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return delete;
    }

    @Override
    public <T> int delete(Class<T> clazz, String where, String[] whereArgs) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int delete = adapter.delete(clazz, where, whereArgs);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return delete;
    }

    @Override
    public <T> int count(T where) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int count = adapter.count(where);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public <T> int count(Class<T> clazz) {
        SqlAdapter adapter = Persistence.getSqliteAdapter(mContext, mDbName);
        int count = adapter.count(clazz);
        try {
            adapter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("QuickSqlAdapter does not have an implementation of this method");
    }
}
