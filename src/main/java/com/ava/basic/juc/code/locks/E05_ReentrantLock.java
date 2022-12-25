package com.ava.basic.juc.code.locks;

/**
 * synchronized提供了一种独占的加锁方式，获取和释放锁由JVM实现
 * 缺点：
 *  1. 当线程获取锁的时候，如果获取不到的锁会一直阻塞，这个阻塞的过程用户无法控制。
 *  2. 如果获取锁的线程进入休眠或者阻塞，除非当前线程异常，否则其他线程尝试获取锁必须一直等待。
 * Lock类弥补了synchronized的局限，提供了更加细粒度的加锁功能。
 * <p>
 * ReentrantLock是Lock的默认实现：
 *  1. 与synchronized都是可重入锁。
 *  2. synchronized在获取锁的过程中，是不可中断的。
 *  3. synchronized是非公平锁，ReentrantLock默认是非公平锁，也可是公平锁（new ReentrantLock(true)）。
 */
public class E05_ReentrantLock {

    public static void main(String[] args) {

        // 释放锁的操作必须放在finally中执行
    }
}
