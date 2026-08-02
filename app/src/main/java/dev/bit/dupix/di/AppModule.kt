package dev.bit.dupix.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.bit.dupix.data.engine.CachingHasher
import dev.bit.dupix.data.local.DupixDatabase
import dev.bit.dupix.data.local.HashCacheDao
import dev.bit.dupix.data.scanner.ContentStreamOpener
import dev.bit.dupix.domain.engine.FileHasher
import dev.bit.dupix.domain.engine.Hasher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DupixDatabase =
        Room.databaseBuilder(context, DupixDatabase::class.java, "dupix.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHashCacheDao(db: DupixDatabase): HashCacheDao = db.hashCacheDao()

    @Provides
    fun provideTrashDao(db: DupixDatabase): dev.bit.dupix.data.local.TrashDao = db.trashDao()

    @Provides
    @Singleton
    fun provideHasher(resolver: ContentResolver, dao: HashCacheDao): Hasher =
        CachingHasher(delegate = FileHasher(ContentStreamOpener(resolver)), dao = dao)
}
