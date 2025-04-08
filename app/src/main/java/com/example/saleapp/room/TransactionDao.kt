package com.example.saleapp.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.saleapp.model.Transaction

// app/src/main/java/com/example/saleapp/room/TransactionDao.kt
@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Query("UPDATE transactions SET status = :status, paymentType = :paymentType WHERE id = :id")
    suspend fun updateStatusAndType(id: Int, status: Int, paymentType: Int)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transactions ORDER BY id DESC LIMIT 1")
    suspend fun getLastTransaction(): Transaction?
}