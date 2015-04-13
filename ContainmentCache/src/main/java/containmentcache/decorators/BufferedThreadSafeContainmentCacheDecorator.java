package containmentcache.decorators;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;

import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;

/**
 * Thread safe cache decorator that buffers addition to streamline entry to the cache
 * This means most 'read' operations perform a full traversal of the buffer as well as the cache.
 * So it is advised to keep it small.
 * 
 * Thread safety is provided by read/write reentrant locks.
 * 
 * @author afrechet
 *
 * @param <E> - type of elements in set representing entry.
 * @param <C> - type of cache entry.
 */
@Deprecated
public class BufferedThreadSafeContainmentCacheDecorator<E,C extends ICacheEntry<E>> implements IContainmentCache<E, C> {
	
	private final IContainmentCache<E,C> fCache;
	private final ReadWriteLock fLock;
	
	private final BlockingQueue<C> fAddBuffer;
	private final int fAddBufferSize;
	private final Thread fAddThread;
	
	private class AddThread extends Thread
	{
		@Override
		public void run()
		{
			
			fLock.writeLock().lock();
			try
			{
				while(fAddBuffer.size() > fAddBufferSize)
				{
					C set;
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
			
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new IllegalStateException("Interrupted while waiting until the next add.",e);
			}
		}
	}
	
	public BufferedThreadSafeContainmentCacheDecorator(IContainmentCache<E,C> cache, ReadWriteLock lock, int addsize)
	{
		fCache = cache;
		fLock = lock;
		
		fAddBuffer = new LinkedBlockingQueue<C>();
		fAddBufferSize = addsize;
		fAddThread = new AddThread();
		fAddThread.start();
	}
	
	@Override
	public void add(C set) {
		fAddBuffer.add(set);
		if(fAddBuffer.size() > fAddBufferSize)
		{
			fAddThread.notify();
		}
	}

	@Override
	public void remove(C set) {
		fLock.writeLock().lock();
		try
		{
			fCache.remove(set);
			fAddBuffer.remove(set);
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
		/**
		 * TODO Implement method, dealing with duplicate sets in buffer + cache.
		 */
		throw new UnsupportedOperationException("Not implemented.");
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
		/**
		 * TODO Implement method, dealing with duplicate sets in buffer + cache.
		 */
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public int size() {
		/**
		 * TODO Implement method, dealing with duplicate sets in buffer + cache.
		 */
		throw new UnsupportedOperationException("Not implemented.");
	}

	

	
	
}
