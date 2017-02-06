package com.konkerlabs.platform.registry.test.business.services;

import static com.konkerlabs.platform.registry.test.base.matchers.ServiceResponseMatchers.hasErrorMessage;
import static com.konkerlabs.platform.registry.test.base.matchers.ServiceResponseMatchers.isResponseOk;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.konkerlabs.platform.registry.business.model.User;
import com.konkerlabs.platform.registry.business.model.enumerations.DateFormat;
import com.konkerlabs.platform.registry.business.model.enumerations.Language;
import com.konkerlabs.platform.registry.business.model.enumerations.TimeZone;
import com.konkerlabs.platform.registry.business.repositories.UserRepository;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponseBuilder;
import com.konkerlabs.platform.registry.business.services.api.UploadService;
import com.konkerlabs.platform.registry.business.services.api.UserService;
import com.konkerlabs.platform.registry.config.CdnConfig;
import com.konkerlabs.platform.registry.config.PasswordUserConfig;
import com.konkerlabs.platform.registry.test.base.BusinessLayerTestSupport;
import com.konkerlabs.platform.registry.test.base.BusinessTestConfiguration;
import com.konkerlabs.platform.registry.test.base.MongoTestConfiguration;
import com.konkerlabs.platform.registry.test.base.RedisTestConfiguration;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        MongoTestConfiguration.class,
        BusinessTestConfiguration.class,
        RedisTestConfiguration.class,
        CdnConfig.class,
        PasswordUserConfig.class
})
@UsingDataSet(locations = {
        "/fixtures/tenants.json",
        "/fixtures/users.json",
        "/fixtures/passwordBlacklist.json"
})
public class UserServiceTest extends BusinessLayerTestSupport {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Mock
    private UploadService uploadService;

    private User user;
    private static final String oldPassword="abc123456789$$";
    private static final String oldPasswordWrong="password";
    private static final String newPassword="123456789abc$$";
    private static final String newPasswordWrong="123456789abc";
    private static final String newPasswordblackListed="aaaaaaaaaaaa";
    private static final String newPasswordConfirmation="123456789abc$$";
    private static final String newPasswordConfirmationWrong="abc124$$";


    @Before
    public void setUp() throws Exception {
    	MockitoAnnotations.initMocks(this);

        user = userRepository.findOne("admin@konkerlabs.com");
    }

    @After
    public void tearDown() throws Exception {
    	Mockito.reset(uploadService);
    }

    @Test
    public void shouldReturnErrorForInvalidUserEmail(){
        user.setEmail("goWrong@noway.com");
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                hasErrorMessage(UserService.Validations.INVALID_USER_EMAIL.getCode()));
    }

    @Test
    public void shouldReturnErrorForInvalidPassword(){
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPasswordWrong,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                hasErrorMessage(UserService.Validations.INVALID_PASSWORD_INVALID.getCode()));
    }

    @Test
    public void shouldReturnErrorForInvalidNewPassword(){
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPasswordWrong, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                hasErrorMessage(UserService.Validations.INVALID_PASSWORD_CONFIRMATION.getCode()));
    }

    @Test
    public void shouldReturnErrorForInvalidNewPasswordConfirmation(){
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmationWrong);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                hasErrorMessage(UserService.Validations.INVALID_PASSWORD_CONFIRMATION.getCode()));
    }

    @Test
    public void shouldReturnErrorForInvalidNewPasswordBlackListed(){
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPasswordblackListed, newPasswordblackListed);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                hasErrorMessage(UserService.Validations.INVALID_PASSWORD_BLACKLISTED.getCode()));
    }

    @Test
    public void shouldReturnErrorForInvalidUserLanguage(){
        user.setLanguage(null);
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                hasErrorMessage(UserService.Validations.INVALID_USER_PREFERENCE_LANGUAGE.getCode()));
    }

    @Test
    public void shouldReturnErrorForInvalidUserLocale(){
        user.setZoneId(null);
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                hasErrorMessage(UserService.Validations.INVALID_USER_PREFERENCE_LOCALE.getCode()));
    }

    @Test
    public void shouldReturnErrorForInvalidUserDateFormat(){
        user.setDateFormat(null);
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                hasErrorMessage(UserService.Validations.INVALID_USER_PREFERENCE_DATEFORMAT.getCode()));
    }

    @Test
    public void shouldSaveNewPassword(){
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                isResponseOk());

        User updated = userRepository.findOne(user.getEmail());
        assertThat(updated.getPassword(), !equals(user.getPassword()));
    }

    @Test
    public void shouldSaveName() {
        user.setName("newName");
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                isResponseOk());

        User updated = userRepository.findOne(user.getEmail());
        assertThat(updated.getName(), !equals(user.getName()));
    }

    @Test
    public void shouldSaveAvatar() throws Exception {
    	
    	uploadService = Mockito.mock(UploadService.class);
		when(uploadService.upload(any(InputStream.class), anyString(), anyString(), anyBoolean()))
				.thenReturn(ServiceResponseBuilder.<String>ok().withResult("d7ATmlS0Xq").build());
		
		Field myField = userService.getClass().getDeclaredField("uploadService"); // fails here
		myField.setAccessible(true);
		myField.set(userService, uploadService);

        user.setAvatar("data:image/png;base64,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");
        ServiceResponse<User> serviceResponse = userService.save(user, oldPassword, newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(serviceResponse, isResponseOk());

        User updated = userRepository.findOne(user.getEmail());
        Assert.assertEquals(updated.getAvatar(), "d7ATmlS0Xq");

        user.setAvatar(null);
    }

    @Test
    public void shouldSaveInvalidAvatar() throws Exception {

		Field myField = userService.getClass().getDeclaredField("uploadService"); // fails here
		myField.setAccessible(true);
		myField.set(userService, uploadService);

        user.setAvatar("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgA/Bw5Okn79cgqvAAAAAElFTkSuQmCC");
        ServiceResponse<User> serviceResponse = userService.save(user, oldPassword, newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(serviceResponse, hasErrorMessage("service.user.validation.avatar.invalid"));

        user.setAvatar(null);
    }

    
    @Test
    public void shouldLocale() {
        user.setZoneId(TimeZone.AMERICA_LOS_ANGELES);
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                isResponseOk());

        User updated = userRepository.findOne(user.getEmail());
        assertThat(updated.getZoneId().getCode(), !equals(user.getZoneId().getCode()));
    }

    @Test
    public void shouldSaveLanguage() {
        user.setLanguage(Language.PT_BR);
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                isResponseOk());

        User updated = userRepository.findOne(user.getEmail());
        assertThat(updated.getLanguage().getCode(), !equals(user.getLanguage().getCode()));
    }

    @Test
    public void shouldSaveDateFormat() {
        user.setDateFormat(DateFormat.MMDDYYYY);
        ServiceResponse<User> serviceResponse =
                userService.save(user, oldPassword,
                        newPassword, newPasswordConfirmation);
        Assert.assertNotNull(serviceResponse);
        assertThat(
                serviceResponse,
                isResponseOk());

        User updated = userRepository.findOne(user.getEmail());
        assertThat(updated.getDateFormat().getCode(), !equals(user.getDateFormat().getCode()));
    }
    
    @Test
    public void shouldRaiseAnExceptionIfEmailNull() {
        ServiceResponse<User> serviceResponse = userService.findByEmail(null);
        
        Assert.assertNotNull(serviceResponse);
        assertThat(serviceResponse, hasErrorMessage(UserService.Validations.NO_EXIST_USER.getCode()));

    }
    
    @Test
    public void shouldReturnUser() {
        ServiceResponse<User> serviceResponse = userService.findByEmail("admin@konkerlabs.com");
        
        Assert.assertNotNull(serviceResponse);
        assertThat(serviceResponse, isResponseOk());
        Assert.assertEquals(user, serviceResponse.getResult());

    }
}
