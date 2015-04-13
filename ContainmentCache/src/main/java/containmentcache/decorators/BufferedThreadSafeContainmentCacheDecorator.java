package containmentcache.decorators;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;

/**
 * Thread safe cache decorator that buffers addition to streamline entry to the cache
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
public class BufferedThreadSafeContainmentCacheDecorator<E,C extends ICacheEntry<E>> implements IContainmentCache<E, C> {
	
	private final IContainmentCache<E,C> fCache;
	private final ReadWriteLock fLock;
	
	private final BlockingQueue<C> fAddBuffer;
	private final Semaphore fAddSemaphore;
	
	private final ExecutorService fExecutorService;
	
	public static <E,C extends ICacheEntry<E>> BufferedThreadSafeContainmentCacheDecorator<E, C> makeBufferedThreadSafe(final IContainmentCache<E, C> cache, final int addsize)
	{
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		return new BufferedThreadSafeContainmentCacheDecorator<E,C>(cache, lock, addsize);
	}
	
	public BufferedThreadSafeContainmentCacheDecorator(final IContainmentCache<E,C> cache, final ReadWriteLock lock, final int addsize)
	{
		fCache = cache;
		fLock = lock;
		
		fAddBuffer = new LinkedBlockingQueue<C>();
		fAddSemaphore = new Semaphore(0);
		
		fExecutorService = Executors.newFixedThreadPool(1);
		AddThread addthread = new AddThread(fAddSemaphore, addsize);
		fExecutorService.submit(addthread);
	}
	
	@Override
	public void add(C set) {
		fAddBuffer.add(set);
		fAddSemaphore.release();
	}

	@Override
	public void remove(C set) {
		fLock.writeLock().lock();
		try
		{
			fCache.remove(set);
			
			final boolean frombuffer = fAddBuffer.remove(set);
			if(frombuffer)
			{
				boolean removedpermit = fAddSemaphore.tryAcquire();
				if(!removedpermit)
				{
					throw new IllegalStateException("Removed an item from the buffer without a corresponding semaphore permit.");
				}
			}
		}
		finally
		{
			fLock.writeLock().unlock();
		}
	}

	@Override
	public boolean contains(C set) {
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
				final boolean inbuffer = fAddBuffer.contains(set);
				return inbuffer;
			}
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public HashSet<C> getSubsets(C set) {
		fLock.readLock().lock();
		try
		{
			final HashSet<C> subsets = new HashSet<C>(fCache.getSubsets(set));
			for(C bufferset : fAddBuffer)
			{
				if(set.getElements().containsAll(bufferset.getElements()))
				{
					subsets.add(bufferset);
				}
			}
			return subsets;
			
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public int getNumberSubsets(C set) {
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
	public HashSet<C> getSupersets(C set) {
		fLock.readLock().lock();
		try
		{
			final HashSet<C> supersets = new HashSet<C>(fCache.getSupersets(set));
			for(C bufferset : fAddBuffer)
			{
				if(bufferset.getElements().containsAll(set.getElements()))
				{
					supersets.add(bufferset);
				}
			}
			return supersets;
		}
		finally
		{
			fLock.readLock().unlock();
		}
	}

	@Override
	public int getNumberSupersets(C set) {
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
		private final Semaphore fSemaphore;
		private final int fAddSize;
		
		public AddThread(Semaphore semaphore, int addsize)
		{
			fSemaphore = semaphore;
			fAddSize = addsize;
		}
		
		@Override
		public void run()
		{
			while(true)
			{
				//Acquire the needed number of permits.
 				try {
					fSemaphore.acquire(fAddSize);
				} catch (InterruptedException e) {
					e.printStackTrace();
					throw new IllegalStateException("Interrupted while waiting for add thread semaphore.",e);
				}
				fLock.writeLock().lock();
				try
				{
					//Do not completely empty the buffer to avoid starving other processes.
					for(int i=0;i<fAddSize;i++)
					{
						if(fAddBuffer.isEmpty())
						{
							throw new IllegalStateException("Expected non-empty queue.");
						}
						
						final C set;
						try {
							set = fAddBuffer.take();
						} catch (InterruptedException e) {
							e.printStackTrace();
							throw new IllegalStateException("Was interrupted while trying to take from the non-empty add queue.",e);
						}
						fCache.add(set);
					}
				}
				finally
				{
					fLock.writeLock().unlock();
				}
			}
		}
	}

	
	
}
