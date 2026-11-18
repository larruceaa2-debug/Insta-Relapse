package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowLensDao {
    @Query("SELECT * FROM snapshots ORDER BY importedAt DESC, id DESC")
    fun observeSnapshots(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM account_records WHERE snapshotId = :snapshotId")
    fun observeRecordsForSnapshot(snapshotId: Long): Flow<List<AccountRecordEntity>>

    @Query("SELECT * FROM account_records WHERE snapshotId = :snapshotId")
    suspend fun getRecordsForSnapshot(snapshotId: Long): List<AccountRecordEntity>

    @Query("SELECT * FROM ignored_accounts ORDER BY addedAt DESC")
    fun observeIgnoredAccounts(): Flow<List<IgnoredAccountEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM ignored_accounts WHERE username = :username)")
    suspend fun isIgnored(username: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AccountRecordEntity>)

    @Transaction
    suspend fun insertSnapshotWithRecords(
        snapshot: SnapshotEntity,
        recordsBuilder: (Long) -> List<AccountRecordEntity>,
    ): SnapshotEntity {
        val snapshotId = insertSnapshot(snapshot)
        val records = recordsBuilder(snapshotId)
        if (records.isNotEmpty()) {
            insertRecords(records)
        }
        return snapshot.copy(id = snapshotId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIgnoredAccount(ignored: IgnoredAccountEntity)

    @Query("DELETE FROM ignored_accounts WHERE username = :username")
    suspend fun deleteIgnoredAccount(username: String)

    @Query("UPDATE snapshots SET accountHandle = :accountHandle WHERE id = :snapshotId")
    suspend fun updateSnapshotAccountHandle(snapshotId: Long, accountHandle: String)

    @Query("DELETE FROM snapshots WHERE id = :snapshotId")
    suspend fun deleteSnapshotById(snapshotId: Long)

    @Query("DELETE FROM snapshots")
    suspend fun deleteAllSnapshots()

    @Query("DELETE FROM ignored_accounts")
    suspend fun deleteAllIgnored()
}
