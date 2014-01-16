package gov.hhs.onc.dcdt.net.impl;

import gov.hhs.onc.dcdt.convert.Converts;
import gov.hhs.onc.dcdt.convert.Converts.List;
import gov.hhs.onc.dcdt.convert.impl.AbstractToolConverter;
import gov.hhs.onc.dcdt.utils.ToolInetAddressUtils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

@Component("inetAddrConv")
@List({ @Converts(from = String[].class, to = InetAddress.class), @Converts(from = String.class, to = InetAddress.class),
    @Converts(from = InetAddress.class, to = String.class) })
@Scope("singleton")
public class InetAddressConverter extends AbstractToolConverter {
    private final static TypeDescriptor TYPE_DESC_INET_ADDR = TypeDescriptor.valueOf(InetAddress.class);

    @Nullable
    @Override
    protected Object convertInternal(Object source, TypeDescriptor sourceType, TypeDescriptor targetType, ConvertiblePair convertPair) {
        if (targetType.isAssignableTo(TYPE_DESC_INET_ADDR)) {
            return ((InetAddress) source).getHostAddress();
        } else if (targetType.isAssignableTo(TYPE_DESC_STR_ARR) || targetType.isAssignableTo(TYPE_DESC_STR)) {
            String[] sourceStrs;

            try {
                return (sourceType.isArray() && (ArrayUtils.getLength(source) == 2)) ? ToolInetAddressUtils.getByAddress((sourceStrs = (String[]) source)[0],
                    sourceStrs[1]) : ToolInetAddressUtils.getByAddress(null, (String) source);
            } catch (UnknownHostException ignored) {
            }
        }

        return null;
    }
}