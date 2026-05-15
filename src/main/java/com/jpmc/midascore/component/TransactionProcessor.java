package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class TransactionProcessor {
    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final IncentiveClient incentiveClient;

    public TransactionProcessor(UserRepository userRepository, TransactionRecordRepository transactionRecordRepository, IncentiveClient incentiveClient) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.incentiveClient = incentiveClient;
    }

    @Transactional
    public void process(Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        if (sender == null || recipient == null || sender.getBalance() < transaction.getAmount()) {
            return;
        }

        Incentive incentive = incentiveClient.getIncentive(transaction);
        float incentiveAmount = incentive == null ? 0 : incentive.getAmount();

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        userRepository.save(sender);
        userRepository.save(recipient);
        transactionRecordRepository.save(new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount));
    }
}
