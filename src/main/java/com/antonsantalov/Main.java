package com.antonsantalov;

import java.util.*;
import java.util.concurrent.*;

public class Main {
    final static private String letters = "abc";
    final static private int TEXT_NUMBER = 10_000;
    final static private int TEXT_LENGTH = 100_000;
    final static private int TEXT_BATCH_SIZE = 100;
    final static private ArrayBlockingQueue<String> aCountingQueue = new ArrayBlockingQueue<>(TEXT_BATCH_SIZE);
    final static private ArrayBlockingQueue<String> bCountingQueue = new ArrayBlockingQueue<>(TEXT_BATCH_SIZE);
    final static private ArrayBlockingQueue<String> cCountingQueue = new ArrayBlockingQueue<>(TEXT_BATCH_SIZE);

    public static void main(String[] args) {
        long startTs = System.currentTimeMillis();

        try (ExecutorService threadPool = Executors.newFixedThreadPool(letters.length() + 1)) {
            final Future<Void> generatingPromise = threadPool.submit(Main::addToQueue);
            final Map<Character, Future<Void>> promiseList = new HashMap<>();
            for (char letter : letters.toCharArray()) {
                promiseList.put(letter, threadPool.submit(() -> countLetter(letter)));
            }

            generatingPromise.get();
            for (char letter : letters.toCharArray()) {
                promiseList.get(letter).get();
            }

            threadPool.shutdown();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        long endTs = System.currentTimeMillis();
        System.out.println("Время: " + (endTs - startTs) + " мс");
    }

    private static Void addToQueue() throws InterruptedException {
        for (int i = 0; i < TEXT_NUMBER; i++) {
            String text = generateText(letters, TEXT_LENGTH);
            aCountingQueue.put(text);
            bCountingQueue.put(text);
            cCountingQueue.put(text);
        }
        return null;
    }

    public static String generateText(String letters, int length) {
        Random random = new Random();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append(letters.charAt(random.nextInt(letters.length())));
        }
        return text.toString();
    }

    private static Void countLetter(char letter) throws InterruptedException {
        ArrayBlockingQueue<String> countingQueue = switch (letter) {
            case 'a' -> aCountingQueue;
            case 'b' -> bCountingQueue;
            case 'c' -> cCountingQueue;
            default -> throw new InterruptedException();
        };
        long maxSize = 0;
        for (int i = 0; i < Main.TEXT_NUMBER; i++) {
            String text = countingQueue.take();
            long curSize = text.chars().filter(c -> c == letter).count();
            maxSize = Math.max(curSize, maxSize);
        }
        System.out.printf("Самое большое количество букв \"%c\" в текстах: %d шт\n", letter, maxSize);
        return null;
    }

}