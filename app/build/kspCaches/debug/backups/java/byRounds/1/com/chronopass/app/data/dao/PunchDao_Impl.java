package com.chronopass.app.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.chronopass.app.data.database.Converters;
import com.chronopass.app.data.entities.Punch;
import com.chronopass.app.data.entities.PunchType;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PunchDao_Impl implements PunchDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Punch> __insertionAdapterOfPunch;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Punch> __deletionAdapterOfPunch;

  private final EntityDeletionOrUpdateAdapter<Punch> __updateAdapterOfPunch;

  private final SharedSQLiteStatement __preparedStmtOfDeleteForEmployee;

  public PunchDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPunch = new EntityInsertionAdapter<Punch>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `punch` (`id`,`employeeId`,`timestamp`,`type`,`latitude`,`longitude`,`accuracy`,`photoPath`,`createdAt`,`editedBy`,`editedAt`,`editReason`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Punch entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getEmployeeId());
        statement.bindLong(3, entity.getTimestamp());
        final String _tmp = __converters.fromType(entity.getType());
        statement.bindString(4, _tmp);
        if (entity.getLatitude() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getLatitude());
        }
        if (entity.getLongitude() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getLongitude());
        }
        if (entity.getAccuracy() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getAccuracy());
        }
        if (entity.getPhotoPath() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getPhotoPath());
        }
        statement.bindLong(9, entity.getCreatedAt());
        if (entity.getEditedBy() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getEditedBy());
        }
        if (entity.getEditedAt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getEditedAt());
        }
        if (entity.getEditReason() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getEditReason());
        }
      }
    };
    this.__deletionAdapterOfPunch = new EntityDeletionOrUpdateAdapter<Punch>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `punch` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Punch entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfPunch = new EntityDeletionOrUpdateAdapter<Punch>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `punch` SET `id` = ?,`employeeId` = ?,`timestamp` = ?,`type` = ?,`latitude` = ?,`longitude` = ?,`accuracy` = ?,`photoPath` = ?,`createdAt` = ?,`editedBy` = ?,`editedAt` = ?,`editReason` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Punch entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getEmployeeId());
        statement.bindLong(3, entity.getTimestamp());
        final String _tmp = __converters.fromType(entity.getType());
        statement.bindString(4, _tmp);
        if (entity.getLatitude() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getLatitude());
        }
        if (entity.getLongitude() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getLongitude());
        }
        if (entity.getAccuracy() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getAccuracy());
        }
        if (entity.getPhotoPath() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getPhotoPath());
        }
        statement.bindLong(9, entity.getCreatedAt());
        if (entity.getEditedBy() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getEditedBy());
        }
        if (entity.getEditedAt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getEditedAt());
        }
        if (entity.getEditReason() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getEditReason());
        }
        statement.bindLong(13, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteForEmployee = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM punch WHERE employeeId = ?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final Punch p) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfPunch.insertAndReturnId(p);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public Object delete(final Punch p, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPunch.handle(p);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Punch p, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPunch.handle(p);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteForEmployee(final long employeeId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteForEmployee.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, employeeId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteForEmployee.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object lastFor(final long employeeId, final Continuation<? super Punch> $completion) {
    final String _sql = "SELECT * FROM punch WHERE employeeId = ? ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, employeeId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Punch>() {
      @Override
      @Nullable
      public Punch call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmployeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "employeeId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEditedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "editedBy");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfEditReason = CursorUtil.getColumnIndexOrThrow(_cursor, "editReason");
          final Punch _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpEmployeeId;
            _tmpEmployeeId = _cursor.getLong(_cursorIndexOfEmployeeId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final PunchType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toType(_tmp);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Float _tmpAccuracy;
            if (_cursor.isNull(_cursorIndexOfAccuracy)) {
              _tmpAccuracy = null;
            } else {
              _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            }
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpEditedBy;
            if (_cursor.isNull(_cursorIndexOfEditedBy)) {
              _tmpEditedBy = null;
            } else {
              _tmpEditedBy = _cursor.getString(_cursorIndexOfEditedBy);
            }
            final Long _tmpEditedAt;
            if (_cursor.isNull(_cursorIndexOfEditedAt)) {
              _tmpEditedAt = null;
            } else {
              _tmpEditedAt = _cursor.getLong(_cursorIndexOfEditedAt);
            }
            final String _tmpEditReason;
            if (_cursor.isNull(_cursorIndexOfEditReason)) {
              _tmpEditReason = null;
            } else {
              _tmpEditReason = _cursor.getString(_cursorIndexOfEditReason);
            }
            _result = new Punch(_tmpId,_tmpEmployeeId,_tmpTimestamp,_tmpType,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpPhotoPath,_tmpCreatedAt,_tmpEditedBy,_tmpEditedAt,_tmpEditReason);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Punch>> between(final long from, final long to) {
    final String _sql = "SELECT * FROM punch WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    _argIndex = 2;
    _statement.bindLong(_argIndex, to);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"punch"}, new Callable<List<Punch>>() {
      @Override
      @NonNull
      public List<Punch> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmployeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "employeeId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEditedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "editedBy");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfEditReason = CursorUtil.getColumnIndexOrThrow(_cursor, "editReason");
          final List<Punch> _result = new ArrayList<Punch>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Punch _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpEmployeeId;
            _tmpEmployeeId = _cursor.getLong(_cursorIndexOfEmployeeId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final PunchType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toType(_tmp);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Float _tmpAccuracy;
            if (_cursor.isNull(_cursorIndexOfAccuracy)) {
              _tmpAccuracy = null;
            } else {
              _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            }
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpEditedBy;
            if (_cursor.isNull(_cursorIndexOfEditedBy)) {
              _tmpEditedBy = null;
            } else {
              _tmpEditedBy = _cursor.getString(_cursorIndexOfEditedBy);
            }
            final Long _tmpEditedAt;
            if (_cursor.isNull(_cursorIndexOfEditedAt)) {
              _tmpEditedAt = null;
            } else {
              _tmpEditedAt = _cursor.getLong(_cursorIndexOfEditedAt);
            }
            final String _tmpEditReason;
            if (_cursor.isNull(_cursorIndexOfEditReason)) {
              _tmpEditReason = null;
            } else {
              _tmpEditReason = _cursor.getString(_cursorIndexOfEditReason);
            }
            _item = new Punch(_tmpId,_tmpEmployeeId,_tmpTimestamp,_tmpType,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpPhotoPath,_tmpCreatedAt,_tmpEditedBy,_tmpEditedAt,_tmpEditReason);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object forEmployeeBetween(final long employeeId, final long from, final long to,
      final Continuation<? super List<Punch>> $completion) {
    final String _sql = "SELECT * FROM punch WHERE employeeId = ? AND timestamp BETWEEN ? AND ? ORDER BY timestamp";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, employeeId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, from);
    _argIndex = 3;
    _statement.bindLong(_argIndex, to);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Punch>>() {
      @Override
      @NonNull
      public List<Punch> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmployeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "employeeId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEditedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "editedBy");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfEditReason = CursorUtil.getColumnIndexOrThrow(_cursor, "editReason");
          final List<Punch> _result = new ArrayList<Punch>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Punch _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpEmployeeId;
            _tmpEmployeeId = _cursor.getLong(_cursorIndexOfEmployeeId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final PunchType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toType(_tmp);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Float _tmpAccuracy;
            if (_cursor.isNull(_cursorIndexOfAccuracy)) {
              _tmpAccuracy = null;
            } else {
              _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            }
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpEditedBy;
            if (_cursor.isNull(_cursorIndexOfEditedBy)) {
              _tmpEditedBy = null;
            } else {
              _tmpEditedBy = _cursor.getString(_cursorIndexOfEditedBy);
            }
            final Long _tmpEditedAt;
            if (_cursor.isNull(_cursorIndexOfEditedAt)) {
              _tmpEditedAt = null;
            } else {
              _tmpEditedAt = _cursor.getLong(_cursorIndexOfEditedAt);
            }
            final String _tmpEditReason;
            if (_cursor.isNull(_cursorIndexOfEditReason)) {
              _tmpEditReason = null;
            } else {
              _tmpEditReason = _cursor.getString(_cursorIndexOfEditReason);
            }
            _item = new Punch(_tmpId,_tmpEmployeeId,_tmpTimestamp,_tmpType,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpPhotoPath,_tmpCreatedAt,_tmpEditedBy,_tmpEditedAt,_tmpEditReason);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object allOnce(final Continuation<? super List<Punch>> $completion) {
    final String _sql = "SELECT * FROM punch ORDER BY timestamp";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Punch>>() {
      @Override
      @NonNull
      public List<Punch> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmployeeId = CursorUtil.getColumnIndexOrThrow(_cursor, "employeeId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEditedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "editedBy");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final int _cursorIndexOfEditReason = CursorUtil.getColumnIndexOrThrow(_cursor, "editReason");
          final List<Punch> _result = new ArrayList<Punch>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Punch _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpEmployeeId;
            _tmpEmployeeId = _cursor.getLong(_cursorIndexOfEmployeeId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final PunchType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toType(_tmp);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Float _tmpAccuracy;
            if (_cursor.isNull(_cursorIndexOfAccuracy)) {
              _tmpAccuracy = null;
            } else {
              _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            }
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpEditedBy;
            if (_cursor.isNull(_cursorIndexOfEditedBy)) {
              _tmpEditedBy = null;
            } else {
              _tmpEditedBy = _cursor.getString(_cursorIndexOfEditedBy);
            }
            final Long _tmpEditedAt;
            if (_cursor.isNull(_cursorIndexOfEditedAt)) {
              _tmpEditedAt = null;
            } else {
              _tmpEditedAt = _cursor.getLong(_cursorIndexOfEditedAt);
            }
            final String _tmpEditReason;
            if (_cursor.isNull(_cursorIndexOfEditReason)) {
              _tmpEditReason = null;
            } else {
              _tmpEditReason = _cursor.getString(_cursorIndexOfEditReason);
            }
            _item = new Punch(_tmpId,_tmpEmployeeId,_tmpTimestamp,_tmpType,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpPhotoPath,_tmpCreatedAt,_tmpEditedBy,_tmpEditedAt,_tmpEditReason);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
