package containmentcache.decorators;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.jcip.annotations.ThreadSafe;

import com.google.common.collect.Iterators;

import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.ILockableContainmentCache;

/**
 * Thread safe cache decorator that buffers 'add' to streamline read methods to the cache.
 * 
 * This means most 'read' operations perform a full traversal of the buffer (checking for elements not already
 * contained in the cache) as well as the cache. So it is advised to keep the buffer size small.
 * 
 * Thread safety is provided by read/write reentrant locks.
 * 
 * @author afrechet
 *
 * @param <E> - type of elements in set representing entry.
 * @param <C> - type of cache entry.
 */
@ThreadSafe
public class BufferedThreadSafeCacheDecorator<E,C extends ICacheEntry<E>> implements ILockableContainmentCache<E, C> {
	
	private final IContainmentCache<E,C> fCache;
	private final ReadWriteLock fLock;
	
	//Must be thread safe.
	private final Set<C> fAddBuffer;
	private final Semaphore fAddSemaphore;
	
	private final ExecutorService fExecutorService;
	
	public static <E,C extends ICacheEntry<E>> ILockableContainmentCache<E, C> makeBufferedThreadSafe(final IContainmentCache<E, C> cache, final int addsize)
	{
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		return new BufferedThreadSafeCacheDecorator<E,C>(cache, lock, addsize);
	}
	
	public BufferedThreadSafeCacheDecorator(final IContainmentCache<E,C> cache, final ReadWriteLock lock, final int addflushsize)
	{
		fCache = cache;
		fLock = lock;
		
		fAddBuffer = Collections.newSetFromMap(new ConcurrentHashMap<C, Boolean>());
		fAddSemaphore = new Semaphore(0);
		
		//Submit add thread.
		fExecutorService = Executors.newFixedThreadPool(1);
		final AddThread addthread = new AddThread(addflushsize);
		fExecutorService.submit(addthread);
		fExecutorService.shutdown();
	}
	
	@Override
	public Lock getReadLock()
	{
		return fLock.readLock();
	}
	
	@Override
	public void add(C set) {
		
		fLock.readLock().lock();
		try
		{
			if(!fCache.contains(set))
			{
				final boolean newelement = fAddBuffer.add(set);
				if(newelement)
				{
					fAddSemaphore.release();
				}
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public void remove(C set) {
		fLock.writeLock().lock();
		try
		{
			final boolean frombuffer = fAddBuffer.remove(set);
			if(frombuffer)
			{
				boolean removedpermit = fAddSemaphore.tryAcquire();
				if(!removedpermit)
				{
					throw new IllegalStateException("Removed an item from the buffer without a corresponding semaphore permit.");
				}
			}
			else
			{
				fCache.remove(set);
			}
		}
		finally
		{
			fLock.writeLock().unlock();
		}
	}

	@Override
	public boolean contains(ICacheEntry<E> set) {
		fLock.readLock().lock();
		try
		{
			//Check if it is in the cache.
			final boolean incache = fCache.contains(set);
			if(incache)
			{
				return true;
			}
			else
			{
				//Check if it is in the buffer.
				return fAddBuffer.contains(set);
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public Iterable<C> getSets() {
		fLock.readLock().lock();
		try
		{
			return new Iterable<C>(){
				@Override
				public Iterator<C> iterator() {
					return Iterators.concat(fCache.getSets().iterator(),fAddBuffer.iterator());
				}};
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}
	
	@Override
	public Iterable<C> getSubsets(ICacheEntry<E> set) {
		fLock.readLock().lock();
		try
		{
			Set<C> buffersubsets = new HashSet<C>();
			for(C bufferset : fAddBuffer)
			{
				if(set.getElements().containsAll(bufferset.getElements()) && !fCache.contains(bufferset))
				{
					buffersubsets.add(bufferset);
				}
			}
			
			return new Iterable<C>(){
				@Override
				public Iterator<C> iterator() {
					
					return Iterators.concat(fCache.getSubsets(set).iterator(),buffersubsets.iterator());
				}};
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public int getNumberSubsets(ICacheEntry<E> set) {
		fLock.readLock().lock();
		try
		{
			int numsubsets = fCache.getNumberSubsets(set);
			final Set<C> uniquebuffer = new HashSet<C>(fAddBuffer);
			for(C bufferset : uniquebuffer)
			{
				if(set.getElements().containsAll(bufferset.getElements()) && !fCache.contains(bufferset))
				{
					numsubsets++;
				}
			}
			return numsubsets;
			
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public Iterable<C> getSupersets(ICacheEntry<E> set) {
		fLock.readLock().lock();
		try
		{
			final HashSet<C> buffersupersets = new HashSet<C>();
			for(C bufferset : fAddBuffer)
			{
				if(bufferset.getElements().containsAll(set.getElements()))
				{
					buffersupersets.add(bufferset);
				}
			}
			return new Iterable<C>(){
				@Override
				public Iterator<C> iterator() {
					
					return Iterators.concat(fCache.getSupersets(set).iterator(),buffersupersets.iterator());
				}};
			
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public int getNumberSupersets(ICacheEntry<E> set) {
		fLock.readLock().lock();
		try
		{
			int numsupersets = fCache.getNumberSupersets(set);
			
			final Set<C> uniquebuffer = new HashSet<C>(fAddBuffer);
			for(C bufferset : uniquebuffer)
			{
				if(bufferset.getElements().containsAll(set.getElements()) && !fCache.contains(bufferset))
				{
					numsupersets++;
				}
			}
			return numsupersets;
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public int size() {
		fLock.readLock().lock();
		try
		{
			int size = fCache.size();
			
			final Set<C> uniquebuffer = new HashSet<C>(fAddBuffer);
			for(C bufferset : uniquebuffer)
			{
				if(!fCache.contains(bufferset))
				{
					size++;
				}
			}
			return size;
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	/**
	 * Runnable in charge of emptying add buffer.
	 * @author afrechet
	 */
	private class AddThread implements Runnable
	{
		private final int fAddFlushSize;
		
		public AddThread(int addflushsize)
		{
			fAddFlushSize = addflushsize;
		}
		
		@Override
		public void run()
		{
			while(true)
			{
				//Acquire the minimum number of permits.
 				try {
 					fAddSemaphore.acquire(fAddFlushSize);
				} catch (InterruptedException e) {
					e.printStackTrace();
					throw new IllegalStateException("Interrupted while waiting for add thread semaphore.",e);
				}
 				
				fLock.writeLock().lock();
				try
				{
					//Flush buffer.
					
					/*
					 * No other thread can alter our state at the moment as we are holding the write lock.
					 * Might as well empty the buffer and drain permits.
					 */
					
					fCache.addAll(fAddBuffer);
					fAddBuffer.clear();
					fAddSemaphore.drainPermits();
				}
				finally
				{
					fLock.writeLock().unlock();
				}
			}
		}
	}
	
}
