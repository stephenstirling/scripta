import { useState, useMemo } from "react";
import { Text, Group, Popover, useMantineTheme } from "@mantine/core";
import { Button } from "@app/ui/Button";
import { useTranslation } from "react-i18next";
import { Icon } from "@app/ui/Icon";
import { Z_INDEX_AUTOMATE_DROPDOWN } from "@app/styles/zIndex";
import { FORMAT_GROUP_LABEL_KEYS } from "@app/constants/convertConstants";
import "@app/components/tools/convert/GroupedFormatDropdown.css";

/** Lists shorter than this fit without scrolling, so they get no search field. */
const SEARCH_THRESHOLD = 10;

interface FormatOption {
  value: string;
  label: string;
  labelKey?: string;
  group: string;
  enabled?: boolean;
  usesCloud?: boolean;
}

interface GroupedFormatDropdownProps {
  value?: string;
  placeholder?: string;
  options: FormatOption[];
  onChange: (value: string) => void;
  disabled?: boolean;
  name?: string;
  withinPortal?: boolean;
  zIndex?: number;
}

const GroupedFormatDropdown = ({
  value,
  placeholder,
  options,
  onChange,
  disabled = false,
  name,
  withinPortal = true,
  zIndex = Z_INDEX_AUTOMATE_DROPDOWN,
}: GroupedFormatDropdownProps) => {
  const { t } = useTranslation();
  const resolvedPlaceholder = placeholder ?? t("dropdownList.selectOption");
  const [dropdownOpened, setDropdownOpened] = useState(false);
  const [query, setQuery] = useState("");
  const theme = useMantineTheme();

  const getOptionLabel = (option: FormatOption) =>
    option.labelKey ? t(option.labelKey, option.label) : option.label;
  const getGroupLabel = (group: string) => {
    const key = FORMAT_GROUP_LABEL_KEYS[group];
    return key ? t(key, group) : group;
  };

  const groupedOptions = useMemo(() => {
    const groups: Record<string, FormatOption[]> = {};

    options.forEach((option) => {
      if (!groups[option.group]) {
        groups[option.group] = [];
      }
      groups[option.group].push(option);
    });

    return groups;
  }, [options]);

  const visibleGroups = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return Object.entries(groupedOptions)
      .map(([group, groupOptions]) => {
        if (!needle || getGroupLabel(group).toLowerCase().includes(needle)) {
          return [group, groupOptions] as const;
        }
        const matches = groupOptions.filter(
          (option) =>
            option.value.includes(needle) ||
            getOptionLabel(option).toLowerCase().includes(needle),
        );
        return [group, matches] as const;
      })
      .filter(([, groupOptions]) => groupOptions.length > 0);
  }, [groupedOptions, query, t]);

  const selectedLabel = useMemo(() => {
    if (!value) return resolvedPlaceholder;
    const selected = options.find((opt) => opt.value === value);
    return selected
      ? `${getGroupLabel(selected.group)} (${getOptionLabel(selected)})`
      : value.toUpperCase();
  }, [value, options, resolvedPlaceholder, t]);

  const close = () => {
    setDropdownOpened(false);
    setQuery("");
  };

  const handleOptionSelect = (selectedValue: string) => {
    onChange(selectedValue);
    close();
  };

  const selectFirstMatch = () => {
    const first = visibleGroups
      .flatMap(([, groupOptions]) => groupOptions)
      .find((option) => option.enabled !== false);
    if (first) handleOptionSelect(first.value);
  };

  return (
    <Popover
      opened={dropdownOpened}
      onDismiss={close}
      position="bottom-start"
      width="26rem"
      withArrow
      shadow="sm"
      disabled={disabled}
      closeOnEscape={true}
      trapFocus
      withinPortal={withinPortal}
      zIndex={zIndex}
    >
      <Popover.Target>
        <Button
          variant="tertiary"
          hover={false}
          fullWidth
          name={name}
          data-testid={name}
          onClick={() => (dropdownOpened ? close() : setDropdownOpened(true))}
          disabled={disabled}
          style={{
            padding: "0.5rem 0.75rem",
            border: `0.0625rem solid ${theme.colors.gray[4]}`,
            borderRadius: theme.radius.sm,
            backgroundColor: disabled
              ? theme.colors.gray[1]
              : "var(--dropdown-trigger-bg)",
            cursor: disabled ? "not-allowed" : "pointer",
            width: "100%",
            color: disabled
              ? "var(--c-text-subtle)"
              : "var(--dropdown-trigger-text)",
          }}
        >
          <Group justify="space-between" style={{ width: "100%" }}>
            <Text size="sm" c={value ? undefined : "dimmed"}>
              {selectedLabel}
            </Text>
            <Icon
              name="chevron-down"
              size={"1rem"}
              style={{
                transform: dropdownOpened ? "rotate(180deg)" : "rotate(0deg)",
                transition: "transform 0.2s ease",
                color: "var(--dropdown-trigger-icon)",
              }}
            />
          </Group>
        </Button>
      </Popover.Target>
      <Popover.Dropdown className="format-menu">
        {options.length >= SEARCH_THRESHOLD && (
          <div className="format-menu__search">
            <Icon name="search" size="0.875rem" />
            <input
              data-autofocus
              value={query}
              placeholder={t("convert.formatSearch", "Find a format")}
              aria-label={t("convert.formatSearch", "Find a format")}
              onChange={(event) => setQuery(event.currentTarget.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  selectFirstMatch();
                }
              }}
            />
          </div>
        )}
        <div className="format-menu__body">
          {visibleGroups.map(([groupName, groupOptions]) => (
            <div className="format-menu__row" key={groupName}>
              <div className="format-menu__group">
                {getGroupLabel(groupName)}
              </div>
              <div className="format-menu__options">
                {groupOptions.map((option) => (
                  <button
                    type="button"
                    key={option.value}
                    className="format-menu__option"
                    data-testid={`format-option-${option.value}`}
                    aria-pressed={value === option.value}
                    disabled={option.enabled === false}
                    onClick={() => handleOptionSelect(option.value)}
                  >
                    {getOptionLabel(option)}
                    {option.usesCloud && <Icon name="cloud" size="0.625rem" />}
                  </button>
                ))}
              </div>
            </div>
          ))}
          {visibleGroups.length === 0 && (
            <div className="format-menu__empty">
              {t("convert.formatNoMatch", "No matching format")}
            </div>
          )}
        </div>
      </Popover.Dropdown>
    </Popover>
  );
};

export default GroupedFormatDropdown;
