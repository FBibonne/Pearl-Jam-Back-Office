package fr.insee.pearljam.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

import fr.insee.pearljam.api.domain.Message;
import fr.insee.pearljam.api.domain.MessageStatus;
import fr.insee.pearljam.api.domain.MessageStatusType;
import fr.insee.pearljam.api.domain.User;
import fr.insee.pearljam.api.domain.Campaign;
import fr.insee.pearljam.api.domain.Interviewer;
import fr.insee.pearljam.api.domain.OrganizationUnit;
import fr.insee.pearljam.api.domain.OUMessageRecipient;
import fr.insee.pearljam.api.domain.OUMessageRecipientId;
import fr.insee.pearljam.api.domain.InterviewerMessageRecipient;

import fr.insee.pearljam.api.repository.MessageRepository;
import fr.insee.pearljam.api.repository.UserRepository;
import fr.insee.pearljam.api.repository.OrganizationUnitRepository;
import fr.insee.pearljam.api.repository.CampaignRepository;
import fr.insee.pearljam.api.repository.InterviewerRepository;


import fr.insee.pearljam.api.dto.message.MessageDto;
import fr.insee.pearljam.api.dto.message.VerifyNameResponseDto;
import fr.insee.pearljam.api.dto.organizationunit.OrganizationUnitDto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


@Service
public class MessageServiceImpl implements MessageService {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

	@Autowired
  MessageRepository messageRepository;
  
  @Autowired
  UserRepository userRepository;
  
  @Autowired
  UserService userService;

  @Autowired
  InterviewerRepository interviewerRepository;
  
  @Autowired
  CampaignRepository campaignRepository;

  @Autowired
  OrganizationUnitRepository organizationUnitRepository;
  
  @Autowired
  private SimpMessagingTemplate brokerMessagingTemplate;


  public HttpStatus markAsRead(Long id, String idep){
    Optional<Interviewer> interv = interviewerRepository.findByIdIgnoreCase(idep);
    Optional<Message> msg = messageRepository.findById(id);
    if(interv.isPresent() && msg.isPresent()){
      LOGGER.info("trying to save");
      Message message = msg.get();
      List<MessageStatus> statusList = message.getMessageStatus();
      if (statusList == null) {
    	  statusList = new ArrayList<>();
      }
	List<MessageStatus> newList = statusList.stream()
    	.filter(c -> c.getInterviewer().getId() != interv.get().getId())
    	.collect(Collectors.toList());
		newList.add(new MessageStatus(message, interv.get(), MessageStatusType.REA));
      message.setMessageStatus(newList);
      messageRepository.save(message);
      return HttpStatus.OK;
    }

    return HttpStatus.NOT_FOUND;
    
  }
  

	public HttpStatus addMessage(String text, List<String> recipients, String userId) {
    Optional<User> optSender = userRepository.findByIdIgnoreCase(userId);
    User sender;
    ArrayList<OrganizationUnit> ouMessageRecipients = new ArrayList<OrganizationUnit>();
    ArrayList<Interviewer> interviewerMessageRecipients = new ArrayList<Interviewer>();
    ArrayList<Campaign> campaignMessageRecipients = new ArrayList<Campaign>();
    List<String> userOUIds = userService.getUserOUs(userId, true)
			.stream().map(ou -> ou.getId()).collect(Collectors.toList());

    if (optSender.isPresent()){
      sender = optSender.get();
    }
    else{
      sender = null;
    }
    Message message = new Message(text, sender, System.currentTimeMillis());
    
    for(String recipient : recipients){
      if (recipient.equals("All")) {
    	  for (String OUId : userOUIds) {
    		  Optional<OrganizationUnit> ouRecipient = organizationUnitRepository.findByIdIgnoreCase(OUId);
              if (ouRecipient.isPresent()){
                ouMessageRecipients.add(ouRecipient.get());
              }
    	  }
      }
      else {
        Optional<Interviewer> interviewerRecipient = interviewerRepository.findByIdIgnoreCase(recipient);
        if (interviewerRecipient.isPresent()){
          interviewerMessageRecipients.add(interviewerRecipient.get());
        }
        else {
            Optional<Campaign> camp = campaignRepository.findByIdIgnoreCase(recipient);
            if (camp.isPresent()){
              campaignMessageRecipients.add(camp.get());
              interviewerMessageRecipients.addAll(interviewerRepository.findInterviewersWorkingOnCampaign(camp.get().getId(), userOUIds));
            }
        }
      }
    }
    
    List<Interviewer> uniqueInterviwerRecipients = interviewerMessageRecipients.stream()
            .collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(Interviewer::getId))),
                                       ArrayList::new));
    message.setInterviewerMessageRecipients(uniqueInterviwerRecipients);
    message.setOuMessageRecipients(ouMessageRecipients);
    message.setCampaignMessageRecipients(campaignMessageRecipients);
    
    for(Interviewer recipient : uniqueInterviwerRecipients){
        LOGGER.info("push to '{}' ", "/notifications/".concat(recipient.getId().toUpperCase()));
        this.brokerMessagingTemplate.convertAndSend("/notifications/".concat(recipient.getId().toUpperCase()), "new message");
      }
    
    for(OrganizationUnit recipient : ouMessageRecipients){
        LOGGER.info("push to '{}' ", "/notifications/".concat(recipient.getId().toUpperCase()));
        this.brokerMessagingTemplate.convertAndSend("/notifications/".concat(recipient.getId().toUpperCase()), "new message");
      }

    messageRepository.save(message);
    return HttpStatus.OK;
	}

  public List<MessageDto> getMessages(String interviewerId) {
    List<Long> ids = messageRepository.getMessageIdsByInterviewer(interviewerId);
    List<OrganizationUnitDto> userOUs = userService.getUserOUs(interviewerId, true);
    List<String> ouIds = userOUs.stream().map(ou -> ou.getId()).collect(Collectors.toList());
    List<Long> idsByOU = messageRepository.getMessageIdsByOrganizationUnit(ouIds);
    

    for(Long id : idsByOU){
      if(!ids.contains(id)){
        ids.add(id);
      }
    }
    List<MessageDto> messages = messageRepository.findMessagesDtoByIds(ids);
    for(MessageDto message : messages) {
    	List<Integer> status = messageRepository.getMessageStatus(message.getId(), interviewerId);
    	if(!status.isEmpty()) {
    		message.setStatus(status.get(0));
    	}
    }
    return messages;
	}
  
  
  public List<MessageDto> getMessageHistory(String userId) {
	  List<String> userOUIds = userService.getUserOUs(userId, true)
				.stream().map(ou -> ou.getId()).collect(Collectors.toList());
	  List<Long> messageIds = messageRepository.getAllOrganizationMessagesIds(userOUIds);
	  
	  List<MessageDto> messages = messageRepository.findMessagesDtoByIds(messageIds);
	    for(MessageDto message : messages) {
	    	List<VerifyNameResponseDto> recipients = messageRepository.getCampaignRecipients(message.getId());
	    	List<String> interviewerIds = new ArrayList<>();
	    	for(VerifyNameResponseDto recipient : recipients) {
	    		interviewerIds.addAll(interviewerRepository.findInterviewersWorkingOnCampaign(recipient.getId(), userOUIds)
	    				.stream().map(Interviewer::getId).collect(Collectors.toList()));
	    	}
	    	recipients.addAll(messageRepository.getInterviewerRecipients(message.getId())
	    			.stream().filter(inter -> !interviewerIds.contains(inter.getId()))
	    			.collect(Collectors.toList()));
	    	
	    	recipients.addAll(messageRepository.getOuRecipients(message.getId()));
	    	
	    	message.setTypedRecipients(recipients);
	    	
	    }
	  
	  
	    return messages;
	}

	public List<VerifyNameResponseDto> verifyName(String text, String userId) {
		List<VerifyNameResponseDto> returnValue = new ArrayList<>();
		
		List<String> userOUIds = userService.getUserOUs(userId, true)
				.stream().map(ou -> ou.getId()).collect(Collectors.toList());
		Pageable topFifteen = PageRequest.of(0, 15);
		returnValue.addAll(interviewerRepository.findMatchingInterviewers(text, userOUIds, topFifteen));
		returnValue.addAll(campaignRepository.findMatchingCampaigns(text, userOUIds, topFifteen));
		return returnValue;

	}
}
